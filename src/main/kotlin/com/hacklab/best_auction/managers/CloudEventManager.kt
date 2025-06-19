package com.hacklab.best_auction.managers

import com.hacklab.best_auction.Main
import com.hacklab.best_auction.data.*
import com.hacklab.best_auction.database.AuctionItems
import com.hacklab.best_auction.database.Bids
import com.hacklab.best_auction.database.CloudSyncStatus
import com.hacklab.best_auction.utils.ItemUtils
import org.bukkit.scheduler.BukkitRunnable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

class CloudEventManager(private val plugin: Main) {
    
    private val httpClient: HttpClient
    private val eventQueue = ConcurrentLinkedQueue<AuctionEventData>()
    private var isProcessing = false
    private var isTokenValid = false
    private var lastTokenValidation = 0L
    
    private val isEnabled: Boolean
        get() = plugin.config.getBoolean("cloud.enabled", false)
    
    private val baseUrl: String
        get() = plugin.config.getString("cloud.base_url", "")!!
    
    private var apiToken: String = plugin.config.getString("cloud.api_token", "")!!
    
    private val eventsEndpoint: String
        get() = "$baseUrl/api/events"
    
    private val validateEndpoint: String
        get() = "$baseUrl/api/auth/validate"
    
    private val syncEndpoint: String
        get() = "$baseUrl/api/sync"
    
    private val serverId: String
        get() = plugin.config.getString("cloud.server_id", "default-server")!!
    
    private val timeout: Long
        get() = plugin.config.getLong("cloud.timeout", 5000)
    
    private val retryAttempts: Int
        get() = plugin.config.getInt("cloud.retry_attempts", 3)
    
    private val retryDelay: Long
        get() = plugin.config.getLong("cloud.retry_delay", 1000)
    
    init {
        httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(timeout))
            .build()
        
        // Validate token on startup if enabled
        if (isEnabled && plugin.config.getBoolean("cloud.auth.validate_on_startup", true)) {
            validateToken().thenAccept { valid ->
                if (valid && plugin.config.getBoolean("cloud.sync.auto_sync_on_startup", true)) {
                    // Start initial data sync if token is valid
                    performInitialSync()
                }
            }
        }
        
        // Start background event processor
        startEventProcessor()
        
        // Start token revalidation timer if enabled
        val revalidateInterval = plugin.config.getInt("cloud.auth.revalidate_interval", 60)
        if (isEnabled && revalidateInterval > 0) {
            startTokenRevalidationTimer(revalidateInterval)
        }
    }
    
    fun sendEvent(eventData: AuctionEventData) {
        if (!isEnabled) {
            plugin.logger.fine("Cloud integration disabled, skipping event: ${eventData.eventType}")
            return
        }
        
        if (baseUrl.isBlank()) {
            plugin.logger.warning("Cloud base URL not configured")
            return
        }
        
        if (apiToken.isBlank()) {
            plugin.logger.warning("Cloud API token not configured")
            return
        }
        
        if (!isTokenValid) {
            plugin.logger.warning("Cloud API token is not valid, skipping event: ${eventData.eventType}")
            return
        }
        
        // Check if this event type is enabled
        val eventTypeEnabled = when (eventData.eventType) {
            AuctionEventType.ITEM_LISTED -> plugin.config.getBoolean("cloud.events.item_listed", true)
            AuctionEventType.BID_PLACED -> plugin.config.getBoolean("cloud.events.bid_placed", true)
            AuctionEventType.BID_CANCELLED -> plugin.config.getBoolean("cloud.events.bid_cancelled", true)
            AuctionEventType.AUCTION_CANCELLED -> plugin.config.getBoolean("cloud.events.auction_cancelled", true)
            AuctionEventType.ITEM_SOLD -> plugin.config.getBoolean("cloud.events.item_sold", true)
        }
        
        if (!eventTypeEnabled) {
            plugin.logger.fine("Event type ${eventData.eventType} is disabled, skipping")
            return
        }
        
        // Add to queue for background processing
        eventQueue.offer(eventData)
        plugin.logger.fine("Queued cloud event: ${eventData.eventType} for auction ${eventData.auctionId}")
    }
    
    private fun startEventProcessor() {
        object : BukkitRunnable() {
            override fun run() {
                processEventQueue()
            }
        }.runTaskTimerAsynchronously(plugin, 20L, 20L) // Run every second
    }
    
    private fun processEventQueue() {
        if (isProcessing || eventQueue.isEmpty()) {
            return
        }
        
        isProcessing = true
        
        try {
            val event = eventQueue.poll()
            if (event != null) {
                sendEventWithRetry(event, 0)
                    .whenComplete { success, throwable ->
                        isProcessing = false
                        if (success) {
                            plugin.logger.fine("Successfully sent cloud event: ${event.eventType}")
                        } else {
                            plugin.logger.warning("Failed to send cloud event after all retries: ${event.eventType}")
                            if (throwable != null) {
                                plugin.logger.warning("Error: ${throwable.message}")
                            }
                        }
                    }
            } else {
                isProcessing = false
            }
        } catch (e: Exception) {
            isProcessing = false
            plugin.logger.warning("Error processing event queue: ${e.message}")
        }
    }
    
    private fun sendEventWithRetry(eventData: AuctionEventData, attempt: Int): CompletableFuture<Boolean> {
        return sendEventHttp(eventData)
            .thenCompose { success ->
                if (success || attempt >= retryAttempts) {
                    CompletableFuture.completedFuture(success)
                } else {
                    // Wait before retry
                    CompletableFuture.delayedExecutor(retryDelay, TimeUnit.MILLISECONDS)
                        .execute { }
                    
                    plugin.logger.fine("Retrying cloud event (attempt ${attempt + 1}/${retryAttempts}): ${eventData.eventType}")
                    sendEventWithRetry(eventData, attempt + 1)
                }
            }
            .exceptionally { throwable ->
                plugin.logger.warning("HTTP request failed: ${throwable.message}")
                if (attempt < retryAttempts) {
                    plugin.logger.fine("Will retry cloud event (attempt ${attempt + 1}/${retryAttempts}): ${eventData.eventType}")
                }
                false
            }
    }
    
    private fun sendEventHttp(eventData: AuctionEventData): CompletableFuture<Boolean> {
        return try {
            val requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(eventsEndpoint))
                .header("Content-Type", "application/json")
                .header("User-Agent", "BestAuction-Plugin/1.0")
                .header("Authorization", "Bearer $apiToken")
                .timeout(Duration.ofMillis(timeout))
                .POST(HttpRequest.BodyPublishers.ofString(eventData.toJson()))
            
            val request = requestBuilder.build()
            
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply { response ->
                    val statusCode = response.statusCode()
                    if (statusCode in 200..299) {
                        plugin.logger.fine("Cloud event sent successfully: ${eventData.eventType}, status: $statusCode")
                        true
                    } else {
                        plugin.logger.warning("Cloud event failed: ${eventData.eventType}, status: $statusCode, body: ${response.body()}")
                        false
                    }
                }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to create HTTP request for cloud event: ${e.message}")
            CompletableFuture.completedFuture(false)
        }
    }
    
    fun shutdown() {
        // Process remaining events in queue before shutdown
        val remainingEvents = mutableListOf<AuctionEventData>()
        while (eventQueue.isNotEmpty()) {
            eventQueue.poll()?.let { remainingEvents.add(it) }
        }
        
        if (remainingEvents.isNotEmpty()) {
            plugin.logger.info("Processing ${remainingEvents.size} remaining cloud events before shutdown...")
            // Note: In a real shutdown, you might want to wait for these to complete
        }
    }
    
    fun validateToken(): CompletableFuture<Boolean> {
        if (!isEnabled || apiToken.isBlank()) {
            isTokenValid = false
            return CompletableFuture.completedFuture(false)
        }
        
        plugin.logger.info("Validating cloud API token...")
        
        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create(validateEndpoint))
                .header("Authorization", "Bearer $apiToken")
                .header("User-Agent", "BestAuction-Plugin/1.0")
                .timeout(Duration.ofMillis(timeout))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build()
            
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply { response ->
                    val statusCode = response.statusCode()
                    val success = statusCode in 200..299
                    
                    if (success) {
                        plugin.logger.info("Cloud API token validation successful")
                        isTokenValid = true
                        lastTokenValidation = System.currentTimeMillis()
                    } else {
                        plugin.logger.warning("Cloud API token validation failed: HTTP $statusCode")
                        plugin.logger.warning("Response: ${response.body()}")
                        isTokenValid = false
                    }
                    
                    success
                }
                .exceptionally { throwable ->
                    plugin.logger.warning("Cloud API token validation error: ${throwable.message}")
                    isTokenValid = false
                    false
                }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to validate cloud API token: ${e.message}")
            isTokenValid = false
            CompletableFuture.completedFuture(false)
        }
    }
    
    private fun startTokenRevalidationTimer(intervalMinutes: Int) {
        object : BukkitRunnable() {
            override fun run() {
                val now = System.currentTimeMillis()
                val intervalMs = intervalMinutes * 60 * 1000L
                
                if (now - lastTokenValidation > intervalMs) {
                    validateToken()
                }
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 60, 20L * 60) // Check every minute
    }
    
    fun getQueueSize(): Int = eventQueue.size
    
    fun isCloudEnabled(): Boolean = isEnabled
    
    fun isTokenValid(): Boolean = isTokenValid
    
    fun forceTokenValidation(): CompletableFuture<Boolean> = validateToken()
    
    fun performInitialSync(): CompletableFuture<SyncResult> {
        if (!isEnabled || !isTokenValid) {
            plugin.logger.warning("Cannot perform initial sync: cloud not enabled or token invalid")
            return CompletableFuture.completedFuture(SyncResult(false, 0, 0, "Cloud not enabled or token invalid"))
        }
        
        plugin.logger.info("Starting initial data synchronization...")
        
        return CompletableFuture.supplyAsync {
            try {
                val lastSync = getLastSyncStatus()
                val forceFullSync = plugin.config.getBoolean("cloud.sync.force_full_sync", false)
                
                if (lastSync != null && lastSync.status == "completed" && !forceFullSync) {
                    plugin.logger.info("Initial sync already completed. Use force_full_sync to override.")
                    return@supplyAsync SyncResult(true, lastSync.syncedAuctions, lastSync.syncedBids, "Already synced")
                }
                
                // Create new sync record
                val syncId = transaction {
                    CloudSyncStatus.insert {
                        it[serverId] = this@CloudEventManager.serverId
                        it[syncType] = "full"
                        it[lastSyncAt] = LocalDateTime.now()
                        it[status] = "in_progress"
                    } get CloudSyncStatus.id
                }
                
                val result = performBatchSync("full")
                
                // Update sync status
                transaction {
                    CloudSyncStatus.update({ CloudSyncStatus.id eq syncId }) {
                        it[status] = if (result.success) "completed" else "failed"
                        it[syncedAuctions] = result.syncedAuctions
                        it[syncedBids] = result.syncedBids
                        it[errorMessage] = result.errorMessage
                    }
                }
                
                if (result.success) {
                    plugin.logger.info("Initial sync completed: ${result.syncedAuctions} auctions, ${result.syncedBids} bids")
                } else {
                    plugin.logger.warning("Initial sync failed: ${result.errorMessage}")
                }
                
                result
            } catch (e: Exception) {
                plugin.logger.warning("Initial sync error: ${e.message}")
                SyncResult(false, 0, 0, e.message)
            }
        }
    }
    
    fun performManualSync(forceFullSync: Boolean = false): CompletableFuture<SyncResult> {
        if (!isEnabled || !isTokenValid) {
            return CompletableFuture.completedFuture(SyncResult(false, 0, 0, "Cloud not enabled or token invalid"))
        }
        
        val syncType = if (forceFullSync) "full" else "incremental"
        plugin.logger.info("Starting manual sync ($syncType)...")
        
        return CompletableFuture.supplyAsync {
            performBatchSync(syncType)
        }
    }
    
    private fun performBatchSync(syncType: String): SyncResult {
        return try {
            val auctions = getAllActiveAuctions()
            val batchSize = plugin.config.getInt("cloud.sync.batch_size", 50)
            val batchDelay = plugin.config.getLong("cloud.sync.batch_delay", 1000)
            
            var totalSyncedAuctions = 0
            var totalSyncedBids = 0
            
            auctions.chunked(batchSize).forEachIndexed { index, batch ->
                if (index > 0) {
                    Thread.sleep(batchDelay)
                }
                
                val request = BatchSyncRequest.create(serverId, syncType, batch)
                val success = sendBatchSync(request)
                
                if (success) {
                    totalSyncedAuctions += batch.size
                    totalSyncedBids += batch.sumOf { it.bids.size }
                    plugin.logger.fine("Synced batch ${index + 1}: ${batch.size} auctions")
                } else {
                    throw Exception("Failed to sync batch ${index + 1}")
                }
            }
            
            SyncResult(true, totalSyncedAuctions, totalSyncedBids)
        } catch (e: Exception) {
            SyncResult(false, 0, 0, e.message)
        }
    }
    
    private fun sendBatchSync(request: BatchSyncRequest): Boolean {
        return try {
            val httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(syncEndpoint))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer $apiToken")
                .header("User-Agent", "BestAuction-Plugin/1.0")
                .timeout(Duration.ofMillis(timeout * 2)) // Longer timeout for sync
                .POST(HttpRequest.BodyPublishers.ofString(request.toJson()))
                .build()
            
            val response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
            val statusCode = response.statusCode()
            
            if (statusCode in 200..299) {
                plugin.logger.fine("Batch sync successful: ${request.auctions.size} auctions")
                true
            } else {
                plugin.logger.warning("Batch sync failed: HTTP $statusCode, Response: ${response.body()}")
                false
            }
        } catch (e: Exception) {
            plugin.logger.warning("Batch sync request failed: ${e.message}")
            false
        }
    }
    
    private fun getAllActiveAuctions(): List<AuctionSyncData> {
        return transaction {
            AuctionItems.select { AuctionItems.isActive eq true }
                .orderBy(AuctionItems.createdAt)
                .mapNotNull { auctionRow ->
                    try {
                        val auctionId = auctionRow[AuctionItems.id].value
                        val item = ItemUtils.deserializeItemStack(auctionRow[AuctionItems.itemData])
                        
                        // Get bids for this auction if enabled
                        val bids = if (plugin.config.getBoolean("cloud.sync.include_bids", true)) {
                            Bids.select { Bids.auctionItem eq auctionId }
                                .orderBy(Bids.createdAt)
                                .map { bidRow ->
                                    BidSyncData(
                                        bidderUuid = bidRow[Bids.bidderUuid],
                                        bidderName = bidRow[Bids.bidderName],
                                        bidAmount = bidRow[Bids.bidAmount],
                                        createdAt = bidRow[Bids.createdAt].format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                    )
                                }
                        } else {
                            emptyList()
                        }
                        
                        AuctionSyncData(
                            auctionId = auctionId,
                            sellerUuid = auctionRow[AuctionItems.sellerUuid],
                            sellerName = auctionRow[AuctionItems.sellerName],
                            itemName = item?.itemMeta?.displayName ?: item?.type?.name ?: "Unknown",
                            itemType = item?.type?.name ?: "UNKNOWN",
                            quantity = auctionRow[AuctionItems.quantity],
                            startPrice = auctionRow[AuctionItems.startPrice],
                            buyoutPrice = auctionRow[AuctionItems.buyoutPrice],
                            currentPrice = auctionRow[AuctionItems.currentPrice],
                            category = auctionRow[AuctionItems.category],
                            listingFee = auctionRow[AuctionItems.listingFee],
                            createdAt = auctionRow[AuctionItems.createdAt].format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                            expiresAt = auctionRow[AuctionItems.expiresAt].format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                            isActive = auctionRow[AuctionItems.isActive],
                            isSold = auctionRow[AuctionItems.isSold],
                            bids = bids,
                            // Add item detail fields
                            itemLore = item?.itemMeta?.lore,
                            itemEnchantments = item?.itemMeta?.enchants?.mapKeys { it.key.key.toString() }?.mapValues { it.value },
                            itemData = if (item != null) ItemUtils.serializeItemStack(item) else null,
                            itemDurability = if (item?.type?.maxDurability != 0.toShort()) item?.durability?.toInt() else null,
                            itemCustomModelData = item?.itemMeta?.customModelData
                        )
                    } catch (e: Exception) {
                        plugin.logger.warning("Failed to process auction ${auctionRow[AuctionItems.id]}: ${e.message}")
                        null
                    }
                }
        }
    }
    
    private fun getLastSyncStatus(): SyncStatusRecord? {
        return transaction {
            CloudSyncStatus.select { CloudSyncStatus.serverId eq this@CloudEventManager.serverId }
                .orderBy(CloudSyncStatus.createdAt to SortOrder.DESC)
                .firstOrNull()?.let { row ->
                    SyncStatusRecord(
                        serverId = row[CloudSyncStatus.serverId],
                        syncType = row[CloudSyncStatus.syncType],
                        lastSyncAt = row[CloudSyncStatus.lastSyncAt],
                        syncedAuctions = row[CloudSyncStatus.syncedAuctions],
                        syncedBids = row[CloudSyncStatus.syncedBids],
                        status = row[CloudSyncStatus.status],
                        errorMessage = row[CloudSyncStatus.errorMessage],
                        createdAt = row[CloudSyncStatus.createdAt]
                    )
                }
        }
    }
    
    fun getSyncStatus(): String {
        val lastSync = getLastSyncStatus()
        return if (lastSync != null) {
            "Last sync: ${lastSync.lastSyncAt} (${lastSync.status}) - ${lastSync.syncedAuctions} auctions, ${lastSync.syncedBids} bids"
        } else {
            "No sync performed yet"
        }
    }
    
    fun updateToken(newToken: String) {
        apiToken = newToken
        isTokenValid = false
        lastTokenValidation = 0L
        plugin.logger.info("Cloud API token updated. Revalidation required.")
    }
}
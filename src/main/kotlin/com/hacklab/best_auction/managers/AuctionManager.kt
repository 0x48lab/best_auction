package com.hacklab.best_auction.managers

import com.hacklab.best_auction.Main
import com.hacklab.best_auction.data.AuctionCategory
import com.hacklab.best_auction.data.AuctionEventData
import com.hacklab.best_auction.data.AuctionItem
import com.hacklab.best_auction.data.Bid
import com.hacklab.best_auction.database.AuctionItems
import com.hacklab.best_auction.database.Bids
import com.hacklab.best_auction.utils.ItemUtils
import net.milkbowl.vault.economy.Economy
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.*

class AuctionManager(private val plugin: Main, private val economy: Economy, private val cloudEventManager: CloudEventManager) {

    fun listItem(player: Player, startPrice: Long, buyoutPrice: Long?): Boolean {
        val item = player.inventory.itemInMainHand
        
        val validation = ItemUtils.isValidForAuction(item, plugin.langManager, player)
        if (!validation.first) {
            player.sendMessage("§c${validation.second}")
            return false
        }
        
        // Check player listing limit
        val maxListings = plugin.config.getInt("auction.max_player_listings", 7)
        val currentListings = getPlayerListings(player.uniqueId).size
        if (currentListings >= maxListings) {
            player.sendMessage("§c${plugin.langManager.getMessage(player, "auction.max_listings_reached", "$maxListings")}") 
            return false
        }
        
        // Check stack quantity limits
        val allowAnyStack = plugin.config.getBoolean("auction.allow_any_stack_quantity", true)
        if (!allowAnyStack) {
            val itemAmount = item.amount
            val maxStackSize = item.type.maxStackSize
            
            // Allow only 1 or max stack size (typically 64)
            if (itemAmount != 1 && itemAmount != maxStackSize) {
                player.sendMessage("§c${plugin.langManager.getMessage(player, "auction.invalid_stack_quantity", "$maxStackSize")}") 
                return false
            }
        }
        
        val isStack = item.amount == item.maxStackSize
        val fee = ItemUtils.calculateFee(startPrice, isStack)
        
        if (!economy.has(player, fee.toDouble())) {
            player.sendMessage("§c${plugin.langManager.getMessage(player, "auction.not_enough_money")}")
            return false
        }
        
        val processedItem = item.clone()
        val category = AuctionCategory.fromMaterial(item.type)
        val durationHours = plugin.config.getInt("auction.default_duration", 228)
        val expiresAt = LocalDateTime.now().plusHours(durationHours.toLong())
        
        plugin.logger.info("Listing item: ${item.type.name} -> Category: ${category.name}")
        if (item.itemMeta?.hasDisplayName() == true) {
            plugin.logger.info("Item has custom name: ${item.itemMeta?.displayName}")
        }
        if (item.itemMeta?.hasEnchants() == true) {
            plugin.logger.info("Item has enchantments: ${item.itemMeta?.enchants?.keys?.map { it.key }}")
        }
        
        return transaction {
            try {
                val auctionId = AuctionItems.insert {
                    it[AuctionItems.sellerUuid] = player.uniqueId.toString()
                    it[AuctionItems.sellerName] = player.name
                    it[AuctionItems.itemData] = ItemUtils.serializeItemStack(processedItem)
                    it[AuctionItems.startPrice] = startPrice
                    it[AuctionItems.buyoutPrice] = buyoutPrice
                    it[AuctionItems.currentPrice] = startPrice
                    it[AuctionItems.category] = category.name
                    it[AuctionItems.listingFee] = fee
                    it[AuctionItems.expiresAt] = expiresAt
                    it[AuctionItems.quantity] = item.amount
                } get AuctionItems.id
                
                if (economy.withdrawPlayer(player, fee.toDouble()).transactionSuccess()) {
                    player.inventory.setItemInMainHand(null)
                    player.sendMessage("§a${plugin.langManager.getMessage(player, "auction.item_listed", "${auctionId.value}")}")
                    player.sendMessage("§7${plugin.langManager.getMessage(player, "auction.listing_fee", "${ItemUtils.formatPriceWithCurrency(fee, economy, plugin)}")}")
                    player.sendMessage("§7${plugin.langManager.getMessage(player, "auction.expires_at", ItemUtils.formatDate(expiresAt, plugin))}")
                    
                    // Send cloud event
                    val eventData = AuctionEventData.createItemListedEvent(
                        serverId = plugin.config.getString("cloud.server_id", "default-server")!!,
                        auctionId = auctionId.value,
                        sellerUuid = player.uniqueId.toString(),
                        sellerName = player.name,
                        itemName = processedItem.itemMeta?.displayName ?: processedItem.type.name,
                        itemType = processedItem.type.name,
                        quantity = processedItem.amount,
                        startPrice = startPrice,
                        buyoutPrice = buyoutPrice
                    )
                    cloudEventManager.sendEvent(eventData)
                    
                    true
                } else {
                    player.sendMessage("§c${plugin.langManager.getMessage(player, "auction.listing_fee_failed")}")
                    false
                }
            } catch (e: Exception) {
                plugin.logger.warning("Failed to list item: ${e.message}")
                player.sendMessage("§c${plugin.langManager.getMessage(player, "auction.listing_failed")}")
                false
            }
        }
    }
    
    fun placeBid(player: Player, itemId: Int, bidAmount: Long): Boolean {
        return transaction {
            val auctionItem = AuctionItems.select { AuctionItems.id eq itemId and AuctionItems.isActive }
                .singleOrNull()
            
            if (auctionItem == null) {
                player.sendMessage("§c${plugin.langManager.getMessage(player, "auction.item_not_found")}")
                return@transaction false
            }
            
            if (auctionItem[AuctionItems.sellerUuid] == player.uniqueId.toString()) {
                player.sendMessage("§c${plugin.langManager.getMessage(player, "auction.cannot_bid_own")}")
                return@transaction false
            }
            
            val currentPrice = auctionItem[AuctionItems.currentPrice]
            val buyoutPrice = auctionItem[AuctionItems.buyoutPrice]
            
            if (bidAmount <= currentPrice) {
                player.sendMessage("§c${plugin.langManager.getMessage(player, "auction.bid_too_low_general")}")
                return@transaction false
            }
            
            if (!economy.has(player, bidAmount.toDouble())) {
                player.sendMessage("§c${plugin.langManager.getMessage(player, "auction.not_enough_money")}")
                return@transaction false
            }
            
            val isBuyout = buyoutPrice != null && bidAmount >= buyoutPrice
            
            if (isBuyout) {
                completeSale(itemId, player.uniqueId, player.name, buyoutPrice!!)
                player.sendMessage("§a${plugin.langManager.getMessage(player, "auction.buyout_success")}")
                
                // Send cloud event for buyout (treated as ITEM_SOLD)
                val sellerUuid = auctionItem[AuctionItems.sellerUuid]
                val sellerName = auctionItem[AuctionItems.sellerName]
                val eventData = AuctionEventData.createItemSoldEvent(
                    serverId = plugin.config.getString("cloud.server_id", "default-server")!!,
                    auctionId = itemId,
                    sellerUuid = sellerUuid,
                    sellerName = sellerName,
                    buyerUuid = player.uniqueId.toString(),
                    buyerName = player.name,
                    finalPrice = buyoutPrice,
                    wasBuyout = true
                )
                cloudEventManager.sendEvent(eventData)
            } else {
                // Remove any existing bid from this player first
                val existingBid = Bids.select { 
                    (Bids.auctionItem eq itemId) and 
                    (Bids.bidderUuid eq player.uniqueId.toString())
                }.orderBy(Bids.createdAt to SortOrder.DESC).firstOrNull()
                
                if (existingBid != null) {
                    val oldBidAmount = existingBid[Bids.bidAmount]
                    // Delete old bid
                    Bids.deleteWhere { 
                        (Bids.auctionItem eq itemId) and 
                        (Bids.bidderUuid eq player.uniqueId.toString())
                    }
                    // Refund old bid amount
                    if (economy.hasAccount(player)) {
                        economy.depositPlayer(player, oldBidAmount.toDouble())
                    }
                    plugin.logger.info("Removed existing bid of $oldBidAmount from player ${player.name} on auction $itemId")
                }
                
                // Add new bid
                Bids.insert {
                    it[Bids.auctionItem] = itemId
                    it[Bids.bidderUuid] = player.uniqueId.toString()
                    it[Bids.bidderName] = player.name
                    it[Bids.bidAmount] = bidAmount
                }
                
                // Withdraw new bid amount
                if (!economy.withdrawPlayer(player, bidAmount.toDouble()).transactionSuccess()) {
                    // If withdrawal fails, remove the bid we just added
                    Bids.deleteWhere { 
                        (Bids.auctionItem eq itemId) and 
                        (Bids.bidderUuid eq player.uniqueId.toString())
                    }
                    player.sendMessage("§c${plugin.langManager.getMessage(player, "auction.withdraw_failed")}")
                    return@transaction false
                }
                
                AuctionItems.update({ AuctionItems.id eq itemId }) {
                    it[AuctionItems.currentPrice] = bidAmount
                }
                
                player.sendMessage("§a${plugin.langManager.getMessage(player, "auction.bid_placed", "${ItemUtils.formatPriceWithCurrency(bidAmount, economy, plugin)}")}")
                
                // Send cloud event for bid
                val eventData = AuctionEventData.createBidPlacedEvent(
                    serverId = plugin.config.getString("cloud.server_id", "default-server")!!,
                    auctionId = itemId,
                    bidderUuid = player.uniqueId.toString(),
                    bidderName = player.name,
                    bidAmount = bidAmount,
                    previousPrice = currentPrice
                )
                cloudEventManager.sendEvent(eventData)
            }
            
            true
        }
    }
    
    fun cancelListing(player: Player, itemId: Int): Boolean {
        return transaction {
            val auctionItem = AuctionItems.select { 
                AuctionItems.id eq itemId and 
                (AuctionItems.sellerUuid eq player.uniqueId.toString()) and
                AuctionItems.isActive and
                not(AuctionItems.isSold)
            }.singleOrNull()
            
            if (auctionItem == null) {
                player.sendMessage("§c${plugin.langManager.getMessage(player, "auction.not_your_auction")}")
                return@transaction false
            }
            
            val hasBids = !Bids.select { Bids.auctionItem eq itemId }.empty()
            if (hasBids) {
                player.sendMessage("§c${plugin.langManager.getMessage(player, "auction.auction_has_bids")}")
                return@transaction false
            }
            
            AuctionItems.update({ AuctionItems.id eq itemId }) {
                it[AuctionItems.isActive] = false
            }
            
            val itemData = auctionItem[AuctionItems.itemData]
            val item = ItemUtils.deserializeItemStack(itemData)
            
            if (item != null) {
                plugin.mailManager.sendMail(
                    UUID.fromString(auctionItem[AuctionItems.sellerUuid]),
                    auctionItem[AuctionItems.sellerName],
                    item,
                    "Cancelled auction listing"
                )
            }
            
            player.sendMessage("§a${plugin.langManager.getMessage(player, "auction.auction_cancelled")}")
            true
        }
    }
    
    fun getActiveListings(category: String? = null, searchTerm: String? = null): List<AuctionItem> {
        return transaction {
            var query = AuctionItems.select { AuctionItems.isActive and not(AuctionItems.isSold) }
            
            if (category != null) {
                query = query.andWhere { AuctionItems.category eq category }
            }
            
            query.orderBy(AuctionItems.currentPrice to SortOrder.ASC, AuctionItems.createdAt to SortOrder.ASC)
                .mapNotNull { row ->
                    val item = ItemUtils.deserializeItemStack(row[AuctionItems.itemData])
                    if (item != null) {
                        val itemName = item.itemMeta?.displayName ?: item.type.name
                        if (searchTerm == null || itemName.contains(searchTerm, ignoreCase = true)) {
                            AuctionItem(
                                id = row[AuctionItems.id].value,
                                sellerUuid = UUID.fromString(row[AuctionItems.sellerUuid]),
                                sellerName = row[AuctionItems.sellerName],
                                itemStack = item,
                                startPrice = row[AuctionItems.startPrice],
                                buyoutPrice = row[AuctionItems.buyoutPrice],
                                currentPrice = row[AuctionItems.currentPrice],
                                category = row[AuctionItems.category],
                                listingFee = row[AuctionItems.listingFee],
                                createdAt = row[AuctionItems.createdAt],
                                expiresAt = row[AuctionItems.expiresAt],
                                quantity = row[AuctionItems.quantity]
                            )
                        } else null
                    } else null
                }
        }
    }
    
    fun getPlayerListings(playerUuid: UUID): List<AuctionItem> {
        return transaction {
            AuctionItems.select { 
                (AuctionItems.sellerUuid eq playerUuid.toString()) and 
                AuctionItems.isActive and 
                not(AuctionItems.isSold) 
            }
            .orderBy(AuctionItems.createdAt to SortOrder.DESC)
            .mapNotNull { row ->
                val item = ItemUtils.deserializeItemStack(row[AuctionItems.itemData])
                if (item != null) {
                    AuctionItem(
                        id = row[AuctionItems.id].value,
                        sellerUuid = UUID.fromString(row[AuctionItems.sellerUuid]),
                        sellerName = row[AuctionItems.sellerName],
                        itemStack = item,
                        startPrice = row[AuctionItems.startPrice],
                        buyoutPrice = row[AuctionItems.buyoutPrice],
                        currentPrice = row[AuctionItems.currentPrice],
                        category = row[AuctionItems.category],
                        listingFee = row[AuctionItems.listingFee],
                        createdAt = row[AuctionItems.createdAt],
                        expiresAt = row[AuctionItems.expiresAt],
                        quantity = row[AuctionItems.quantity]
                    )
                } else null
            }
        }
    }
    
    private fun completeSale(itemId: Int, buyerUuid: UUID, buyerName: String, price: Long) {
        transaction {
            val auctionItem = AuctionItems.select { AuctionItems.id eq itemId }.single()
            val sellerUuid = UUID.fromString(auctionItem[AuctionItems.sellerUuid])
            val sellerName = auctionItem[AuctionItems.sellerName]
            val item = ItemUtils.deserializeItemStack(auctionItem[AuctionItems.itemData])
            
            if (item != null) {
                plugin.mailManager.sendMail(buyerUuid, buyerName, item, "Auction purchase")
                
                val seller = plugin.server.getOfflinePlayer(sellerUuid)
                economy.depositPlayer(seller, price.toDouble())
                
                if (seller.isOnline) {
                    seller.player?.sendMessage("§a${plugin.langManager.getMessage(seller.player!!, "auction.item_sold", "${ItemUtils.formatPriceWithCurrency(price, economy, plugin)}")}")
                }
            }
            
            AuctionItems.update({ AuctionItems.id eq itemId }) {
                it[AuctionItems.isActive] = false
                it[AuctionItems.isSold] = true
            }
            
            // Send cloud event for item sold
            val eventData = AuctionEventData.createItemSoldEvent(
                serverId = plugin.config.getString("cloud.server_id", "default-server")!!,
                auctionId = itemId,
                sellerUuid = sellerUuid.toString(),
                sellerName = sellerName,
                buyerUuid = buyerUuid.toString(),
                buyerName = buyerName,
                finalPrice = price,
                wasBuyout = false
            )
            cloudEventManager.sendEvent(eventData)
        }
    }
    
    fun cancelAuction(player: Player, auctionId: Int): Boolean {
        plugin.logger.info("cancelAuction called for player ${player.name}, auctionId: $auctionId")
        return transaction {
            try {
                // Get auction item
                val auctionRow = AuctionItems.select { 
                    AuctionItems.id eq auctionId and 
                    (AuctionItems.isActive eq true) and 
                    (AuctionItems.isSold eq false)
                }.singleOrNull()
                
                if (auctionRow == null) {
                    plugin.logger.warning("Auction item not found or inactive for auctionId: $auctionId")
                    return@transaction false
                }
                
                val sellerUuid = auctionRow[AuctionItems.sellerUuid]
                plugin.logger.info("Found auction for cancellation: sellerUuid=$sellerUuid, playerUuid=${player.uniqueId}")
                
                // Check if player is the seller
                if (sellerUuid != player.uniqueId.toString()) {
                    plugin.logger.warning("Player ${player.name} tried to cancel auction that's not theirs")
                    player.sendMessage(plugin.langManager.getMessage(player, "auction.not_your_auction"))
                    return@transaction false
                }
                
                // Get item data
                val itemData = auctionRow[AuctionItems.itemData]
                val item = ItemUtils.deserializeItemStack(itemData) ?: return@transaction false
                
                // Get all bidders for this auction
                val bidders = Bids.select { Bids.auctionItem eq auctionId }
                    .orderBy(Bids.createdAt, SortOrder.DESC)
                    .map { 
                        Triple(
                            UUID.fromString(it[Bids.bidderUuid]),
                            it[Bids.bidderName],
                            it[Bids.bidAmount]
                        )
                    }
                
                // Refund all bidders
                bidders.forEach { (bidderUuid, bidderName, bidAmount) ->
                    val offlinePlayer = plugin.server.getOfflinePlayer(bidderUuid)
                    if (economy.hasAccount(offlinePlayer)) {
                        economy.depositPlayer(offlinePlayer, bidAmount.toDouble())
                    }
                    
                    // Send mail notification to bidder (no item, just notification)
                    // Note: mailManager.sendMail expects an ItemStack, so we skip this for now
                    
                    // Send message if online
                    val onlineBidder = plugin.server.getPlayer(bidderUuid)
                    if (onlineBidder?.isOnline == true) {
                        onlineBidder.sendMessage(
                            plugin.langManager.getMessage(onlineBidder, "auction.auction_cancelled_notification")
                        )
                    }
                }
                
                // Return item to seller via mailbox
                plugin.mailManager.sendMail(
                    player.uniqueId,
                    player.name,
                    item,
                    plugin.langManager.getMessage(player, "mail.mail_cancelled")
                )
                
                // Mark auction as inactive
                AuctionItems.update({ AuctionItems.id eq auctionId }) {
                    it[AuctionItems.isActive] = false
                }
                
                // Delete all bids (if any)
                // Note: We don't delete bids here as refunds are already processed above
                
                player.sendMessage(plugin.langManager.getMessage(player, "auction.auction_cancelled"))
                plugin.logger.info("Successfully cancelled auction ID: $auctionId for player ${player.name}")
                
                // Send cloud event for auction cancellation
                val eventData = AuctionEventData.createAuctionCancelledEvent(
                    serverId = plugin.config.getString("cloud.server_id", "default-server")!!,
                    auctionId = auctionId,
                    sellerUuid = player.uniqueId.toString(),
                    sellerName = player.name,
                    reason = "cancelled_by_seller"
                )
                cloudEventManager.sendEvent(eventData)
                
                true
            } catch (e: Exception) {
                plugin.logger.warning("Failed to cancel auction: ${e.message}")
                player.sendMessage(plugin.langManager.getMessage(player, "general.unknown_error"))
                false
            }
        }
    }
    
    fun getPlayerBids(playerUuid: UUID): List<AuctionItem> {
        return transaction {
            // Get all active auctions where the player has placed bids
            // Use a subquery to get only the latest bid per auction for this player
            val latestBids = AuctionItems.select { 
                AuctionItems.isActive and not(AuctionItems.isSold)
            }.mapNotNull { auctionRow ->
                // For each active auction, check if player has a bid
                val latestBid = Bids.select { 
                    (Bids.auctionItem eq auctionRow[AuctionItems.id].value) and 
                    (Bids.bidderUuid eq playerUuid.toString())
                }.orderBy(Bids.createdAt to SortOrder.DESC).firstOrNull()
                
                if (latestBid != null) {
                    val item = ItemUtils.deserializeItemStack(auctionRow[AuctionItems.itemData])
                    if (item != null) {
                        AuctionItem(
                            id = auctionRow[AuctionItems.id].value,
                            sellerUuid = UUID.fromString(auctionRow[AuctionItems.sellerUuid]),
                            sellerName = auctionRow[AuctionItems.sellerName],
                            itemStack = item,
                            startPrice = auctionRow[AuctionItems.startPrice],
                            buyoutPrice = auctionRow[AuctionItems.buyoutPrice],
                            currentPrice = auctionRow[AuctionItems.currentPrice],
                            category = auctionRow[AuctionItems.category],
                            listingFee = auctionRow[AuctionItems.listingFee],
                            createdAt = auctionRow[AuctionItems.createdAt],
                            expiresAt = auctionRow[AuctionItems.expiresAt],
                            quantity = auctionRow[AuctionItems.quantity],
                            playerBidAmount = latestBid[Bids.bidAmount] // Player's latest bid amount
                        )
                    } else null
                } else null
            }
            
            // Sort by creation date (most recent first)
            latestBids.sortedByDescending { it.createdAt }
        }
    }
    
    fun cancelPlayerBid(player: Player, auctionId: Int): Boolean {
        return transaction {
            try {
                // Check if the auction exists and is still active
                val auctionItem = AuctionItems.select { 
                    AuctionItems.id eq auctionId and 
                    AuctionItems.isActive and 
                    not(AuctionItems.isSold)
                }.singleOrNull()
                
                if (auctionItem == null) {
                    player.sendMessage("§c${plugin.langManager.getMessage(player, "auction.item_not_found")}")
                    return@transaction false
                }
                
                // Check if player has a bid on this auction
                val playerBid = Bids.select { 
                    (Bids.auctionItem eq auctionId) and 
                    (Bids.bidderUuid eq player.uniqueId.toString())
                }.orderBy(Bids.createdAt to SortOrder.DESC).firstOrNull()
                
                if (playerBid == null) {
                    player.sendMessage("§c${plugin.langManager.getMessage(player, "auction.no_bid_found")}")
                    return@transaction false
                }
                
                val bidAmount = playerBid[Bids.bidAmount]
                
                // Check if this player has the highest bid
                val highestBid = Bids.select { Bids.auctionItem eq auctionId }
                    .orderBy(Bids.bidAmount to SortOrder.DESC, Bids.createdAt to SortOrder.DESC)
                    .firstOrNull()
                
                if (highestBid != null && highestBid[Bids.bidderUuid] == player.uniqueId.toString()) {
                    // Player has the highest bid, need to find the second highest
                    val secondHighestBid = Bids.select { 
                        (Bids.auctionItem eq auctionId) and 
                        not(Bids.bidderUuid eq player.uniqueId.toString())
                    }.orderBy(Bids.bidAmount to SortOrder.DESC, Bids.createdAt to SortOrder.DESC)
                        .firstOrNull()
                    
                    if (secondHighestBid != null) {
                        // Update auction with second highest bid
                        AuctionItems.update({ AuctionItems.id eq auctionId }) {
                            it[AuctionItems.currentPrice] = secondHighestBid[Bids.bidAmount]
                        }
                    } else {
                        // No other bids, revert to starting price
                        AuctionItems.update({ AuctionItems.id eq auctionId }) {
                            it[AuctionItems.currentPrice] = auctionItem[AuctionItems.startPrice]
                        }
                    }
                }
                
                // Remove player's bid
                Bids.deleteWhere { (Bids.auctionItem eq auctionId) and (Bids.bidderUuid eq player.uniqueId.toString()) }
                
                // Refund the bid amount
                if (economy.hasAccount(player)) {
                    economy.depositPlayer(player, bidAmount.toDouble())
                }
                
                player.sendMessage("§a${plugin.langManager.getMessage(player, "auction.bid_cancelled_success", "${ItemUtils.formatPriceWithCurrency(bidAmount, economy, plugin)}")}")
                plugin.logger.info("Player ${player.name} cancelled bid of $bidAmount on auction $auctionId")
                
                // Send cloud event for bid cancellation
                val eventData = AuctionEventData.createBidCancelledEvent(
                    serverId = plugin.config.getString("cloud.server_id", "default-server")!!,
                    auctionId = auctionId,
                    bidderUuid = player.uniqueId.toString(),
                    bidderName = player.name,
                    refundAmount = bidAmount
                )
                cloudEventManager.sendEvent(eventData)
                
                true
            } catch (e: Exception) {
                plugin.logger.warning("Failed to cancel player bid: ${e.message}")
                player.sendMessage("§c${plugin.langManager.getMessage(player, "general.unknown_error")}")
                false
            }
        }
    }
    
    fun getAuctionInfo(auctionId: Int, playerUuid: UUID? = null): AuctionItem? {
        return transaction {
            try {
                val auctionRow = AuctionItems.select { 
                    AuctionItems.id eq auctionId and 
                    AuctionItems.isActive and 
                    not(AuctionItems.isSold)
                }.singleOrNull()
                
                if (auctionRow != null) {
                    val item = ItemUtils.deserializeItemStack(auctionRow[AuctionItems.itemData])
                    if (item != null) {
                        // Get player's bid amount if playerUuid is provided
                        val playerBidAmount = if (playerUuid != null) {
                            Bids.select { 
                                (Bids.auctionItem eq auctionId) and 
                                (Bids.bidderUuid eq playerUuid.toString())
                            }.orderBy(Bids.createdAt to SortOrder.DESC)
                                .firstOrNull()?.get(Bids.bidAmount)
                        } else null
                        
                        AuctionItem(
                            id = auctionRow[AuctionItems.id].value,
                            sellerUuid = UUID.fromString(auctionRow[AuctionItems.sellerUuid]),
                            sellerName = auctionRow[AuctionItems.sellerName],
                            itemStack = item,
                            startPrice = auctionRow[AuctionItems.startPrice],
                            buyoutPrice = auctionRow[AuctionItems.buyoutPrice],
                            currentPrice = auctionRow[AuctionItems.currentPrice],
                            category = auctionRow[AuctionItems.category],
                            listingFee = auctionRow[AuctionItems.listingFee],
                            createdAt = auctionRow[AuctionItems.createdAt],
                            expiresAt = auctionRow[AuctionItems.expiresAt],
                            quantity = auctionRow[AuctionItems.quantity],
                            playerBidAmount = playerBidAmount
                        )
                    } else null
                } else null
            } catch (e: Exception) {
                plugin.logger.warning("Failed to get auction info for ID $auctionId: ${e.message}")
                null
            }
        }
    }

    // Test data generation method
    fun createAuctionItem(
        sellerUuid: UUID,
        sellerName: String,
        itemStack: ItemStack,
        startingPrice: Long,
        currentPrice: Long,
        buyoutPrice: Long?,
        category: String
    ): Int? {
        val expiresAt = LocalDateTime.now().plusDays(7).plusHours(kotlin.random.Random.nextLong(1, 24))
        
        return transaction {
            try {
                val auctionId = AuctionItems.insert {
                    it[AuctionItems.sellerUuid] = sellerUuid.toString()
                    it[AuctionItems.sellerName] = sellerName
                    it[AuctionItems.itemData] = ItemUtils.serializeItemStack(itemStack)
                    it[AuctionItems.startPrice] = startingPrice
                    it[AuctionItems.buyoutPrice] = buyoutPrice
                    it[AuctionItems.currentPrice] = currentPrice
                    it[AuctionItems.category] = category
                    it[AuctionItems.listingFee] = 0L // No fee for test data
                    it[AuctionItems.expiresAt] = expiresAt
                    it[AuctionItems.quantity] = itemStack.amount
                } get AuctionItems.id
                
                auctionId.value
            } catch (e: Exception) {
                plugin.logger.warning("Failed to create test auction item: ${e.message}")
                null
            }
        }
    }
}
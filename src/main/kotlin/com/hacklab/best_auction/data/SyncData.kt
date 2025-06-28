package com.hacklab.best_auction.data

import com.google.gson.Gson
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class AuctionSyncData(
    val auctionId: Int,
    val sellerUuid: String,
    val sellerName: String,
    val itemName: String,
    val itemType: String,
    val quantity: Int,
    val startPrice: Long,
    val buyoutPrice: Long?,
    val currentPrice: Long,
    val category: String,
    val listingFee: Long,
    val createdAt: String,
    val expiresAt: String,
    val isActive: Boolean,
    val isSold: Boolean,
    val bids: List<BidSyncData> = emptyList(),
    // New item detail fields
    val itemLore: List<String>? = null,
    val itemEnchantments: Map<String, Int>? = null,
    val itemData: String? = null, // Base64 serialized ItemStack
    val itemDurability: Int? = null,
    val itemCustomModelData: Int? = null
)

data class BidSyncData(
    val bidderUuid: String,
    val bidderName: String,
    val bidAmount: Long,
    val createdAt: String
)

data class BatchSyncRequest(
    val serverId: String,
    val syncType: String, // "full" or "incremental"
    val timestamp: String,
    val auctions: List<AuctionSyncData>
) {
    fun toJson(): String {
        val gson = Gson()
        return gson.toJson(this)
    }
    
    companion object {
        fun create(
            serverId: String,
            syncType: String,
            auctions: List<AuctionSyncData>
        ): BatchSyncRequest {
            return BatchSyncRequest(
                serverId = serverId,
                syncType = syncType,
                timestamp = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT),
                auctions = auctions
            )
        }
    }
}

enum class SyncStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}

data class SyncResult(
    val success: Boolean,
    val syncedAuctions: Int,
    val syncedBids: Int,
    val errorMessage: String? = null
)

data class SyncStatusRecord(
    val serverId: String,
    val syncType: String,
    val lastSyncAt: java.time.LocalDateTime,
    val syncedAuctions: Int,
    val syncedBids: Int,
    val status: String,
    val errorMessage: String?,
    val createdAt: java.time.LocalDateTime
)
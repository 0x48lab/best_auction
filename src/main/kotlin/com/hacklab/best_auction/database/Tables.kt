package com.hacklab.best_auction.database

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object AuctionItems : IntIdTable("auction_items") {
    val sellerUuid = varchar("seller_uuid", 36)
    val sellerName = varchar("seller_name", 16)
    val itemData = text("item_data")
    val startPrice = long("start_price")
    val buyoutPrice = long("buyout_price").nullable()
    val currentPrice = long("current_price")
    val category = varchar("category", 50)
    val listingFee = long("listing_fee")
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val expiresAt = datetime("expires_at")
    val isActive = bool("is_active").default(true)
    val isSold = bool("is_sold").default(false)
    val quantity = integer("quantity").default(1)
}

object Bids : IntIdTable("bids") {
    val auctionItem = reference("auction_item_id", AuctionItems, onDelete = ReferenceOption.CASCADE)
    val bidderUuid = varchar("bidder_uuid", 36)
    val bidderName = varchar("bidder_name", 16)
    val bidAmount = long("bid_amount")
    val createdAt = datetime("created_at").default(LocalDateTime.now())
}

object MailBox : IntIdTable("mailbox") {
    val playerUuid = varchar("player_uuid", 36)
    val playerName = varchar("player_name", 16)
    val itemData = text("item_data")
    val reason = varchar("reason", 100)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val isCollected = bool("is_collected").default(false)
}

object AuctionSettings : IntIdTable("auction_settings") {
    val playerUuid = varchar("player_uuid", 36)
    val setting = varchar("setting", 50)
    val value = varchar("value", 255)
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
}

object PlayerLanguageSettings : IntIdTable("player_language_settings") {
    val playerUuid = varchar("player_uuid", 36).uniqueIndex()
    val language = varchar("language", 10).default("auto")
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
}

object CloudSyncStatus : IntIdTable("cloud_sync_status") {
    val serverId = varchar("server_id", 50)
    val syncType = varchar("sync_type", 20) // 'full', 'incremental'
    val lastSyncAt = datetime("last_sync_at")
    val syncedAuctions = integer("synced_auctions").default(0)
    val syncedBids = integer("synced_bids").default(0)
    val status = varchar("status", 20).default("pending") // 'pending', 'in_progress', 'completed', 'failed'
    val errorMessage = text("error_message").nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())
}
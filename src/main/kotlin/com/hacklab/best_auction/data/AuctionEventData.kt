package com.hacklab.best_auction.data

import com.google.gson.Gson
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

enum class AuctionEventType {
    ITEM_LISTED,
    BID_PLACED,
    BID_CANCELLED,
    AUCTION_CANCELLED,
    ITEM_SOLD
}

data class AuctionEventData(
    val eventType: AuctionEventType,
    val serverId: String,
    val timestamp: String,
    val auctionId: Int,
    val data: Map<String, Any>
) {
    companion object {
        private val gson = Gson()
        
        fun createItemListedEvent(
            serverId: String,
            auctionId: Int,
            sellerUuid: String,
            sellerName: String,
            itemName: String,
            itemType: String,
            quantity: Int,
            startPrice: Long,
            buyoutPrice: Long?,
            createdAt: String,
            expiresAt: String
        ): AuctionEventData {
            val data = mutableMapOf<String, Any>(
                "seller_uuid" to sellerUuid,
                "seller_name" to sellerName,
                "item_name" to itemName,
                "item_type" to itemType,
                "quantity" to quantity,
                "start_price" to startPrice,
                "created_at" to createdAt,
                "expires_at" to expiresAt
            )
            if (buyoutPrice != null) {
                data["buyout_price"] = buyoutPrice
            }
            
            return AuctionEventData(
                eventType = AuctionEventType.ITEM_LISTED,
                serverId = serverId,
                timestamp = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT),
                auctionId = auctionId,
                data = data
            )
        }
        
        fun createBidPlacedEvent(
            serverId: String,
            auctionId: Int,
            bidderUuid: String,
            bidderName: String,
            bidAmount: Long,
            previousPrice: Long
        ): AuctionEventData {
            val data = mapOf<String, Any>(
                "bidder_uuid" to bidderUuid,
                "bidder_name" to bidderName,
                "bid_amount" to bidAmount,
                "previous_price" to previousPrice
            )
            
            return AuctionEventData(
                eventType = AuctionEventType.BID_PLACED,
                serverId = serverId,
                timestamp = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT),
                auctionId = auctionId,
                data = data
            )
        }
        
        fun createBidCancelledEvent(
            serverId: String,
            auctionId: Int,
            bidderUuid: String,
            bidderName: String,
            refundAmount: Long
        ): AuctionEventData {
            val data = mapOf<String, Any>(
                "bidder_uuid" to bidderUuid,
                "bidder_name" to bidderName,
                "refund_amount" to refundAmount
            )
            
            return AuctionEventData(
                eventType = AuctionEventType.BID_CANCELLED,
                serverId = serverId,
                timestamp = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT),
                auctionId = auctionId,
                data = data
            )
        }
        
        fun createAuctionCancelledEvent(
            serverId: String,
            auctionId: Int,
            sellerUuid: String,
            sellerName: String,
            reason: String = "cancelled_by_seller"
        ): AuctionEventData {
            val data = mapOf<String, Any>(
                "seller_uuid" to sellerUuid,
                "seller_name" to sellerName,
                "reason" to reason
            )
            
            return AuctionEventData(
                eventType = AuctionEventType.AUCTION_CANCELLED,
                serverId = serverId,
                timestamp = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT),
                auctionId = auctionId,
                data = data
            )
        }
        
        fun createItemSoldEvent(
            serverId: String,
            auctionId: Int,
            sellerUuid: String,
            sellerName: String,
            buyerUuid: String,
            buyerName: String,
            finalPrice: Long,
            wasBuyout: Boolean
        ): AuctionEventData {
            val data = mapOf<String, Any>(
                "seller_uuid" to sellerUuid,
                "seller_name" to sellerName,
                "buyer_uuid" to buyerUuid,
                "buyer_name" to buyerName,
                "final_price" to finalPrice,
                "was_buyout" to wasBuyout
            )
            
            return AuctionEventData(
                eventType = AuctionEventType.ITEM_SOLD,
                serverId = serverId,
                timestamp = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT),
                auctionId = auctionId,
                data = data
            )
        }
    }
    
    fun toJson(): String {
        val jsonMap = mapOf(
            "event_type" to eventType.name,
            "server_id" to serverId,
            "timestamp" to timestamp,
            "auction_id" to auctionId,
            "data" to data
        )
        return gson.toJson(jsonMap)
    }
}
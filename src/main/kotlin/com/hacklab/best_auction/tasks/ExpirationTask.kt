package com.hacklab.best_auction.tasks

import com.hacklab.best_auction.Main
import com.hacklab.best_auction.database.AuctionItems
import com.hacklab.best_auction.utils.ItemUtils
import org.bukkit.scheduler.BukkitRunnable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.*

class ExpirationTask(private val plugin: Main) : BukkitRunnable() {
    
    override fun run() {
        transaction {
            val now = LocalDateTime.now()
            val expiredItems = AuctionItems.select { 
                (AuctionItems.isActive eq true) and 
                (AuctionItems.isSold eq false) and 
                (AuctionItems.expiresAt less now)
            }
            
            expiredItems.forEach { row ->
                val itemId = row[AuctionItems.id].value
                val sellerUuid = UUID.fromString(row[AuctionItems.sellerUuid])
                val sellerName = row[AuctionItems.sellerName]
                val itemData = row[AuctionItems.itemData]
                
                val item = ItemUtils.deserializeItemStack(itemData)
                if (item != null) {
                    plugin.mailManager.sendMail(
                        sellerUuid,
                        sellerName,
                        item,
                        "Auction expired - item returned"
                    )
                    
                    val seller = plugin.server.getOfflinePlayer(sellerUuid)
                    if (seller.isOnline) {
                        seller.player?.sendMessage("§cYour auction has expired and the item was sent to your mailbox.")
                    }
                }
                
                AuctionItems.update({ AuctionItems.id eq itemId }) {
                    it[AuctionItems.isActive] = false
                }
                
                plugin.logger.info("Expired auction item $itemId for seller $sellerName")
            }
        }
    }
}
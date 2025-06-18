package com.hacklab.best_auction.managers

import com.hacklab.best_auction.Main
import com.hacklab.best_auction.data.AuctionCategory
import com.hacklab.best_auction.data.AuctionItem
import com.hacklab.best_auction.data.Bid
import com.hacklab.best_auction.database.AuctionItems
import com.hacklab.best_auction.database.Bids
import com.hacklab.best_auction.utils.ItemUtils
import net.milkbowl.vault.economy.Economy
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.*

class AuctionManager(private val plugin: Main, private val economy: Economy) {

    fun listItem(player: Player, startPrice: Long, buyoutPrice: Long?): Boolean {
        val item = player.inventory.itemInMainHand
        
        val validation = ItemUtils.isValidForAuction(item)
        if (!validation.first) {
            player.sendMessage("§c${validation.second}")
            return false
        }
        
        val isStack = item.amount == item.maxStackSize
        val fee = ItemUtils.calculateFee(startPrice, isStack)
        
        if (!economy.has(player, fee.toDouble())) {
            player.sendMessage("§cYou need ${ItemUtils.formatPrice(fee)} gil to list this item!")
            return false
        }
        
        val processedItem = item.clone()
        val category = AuctionCategory.fromMaterial(item.type)
        val expiresAt = LocalDateTime.now().plusDays(9).plusHours(12)
        
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
                }
                
                if (economy.withdrawPlayer(player, fee.toDouble()).transactionSuccess()) {
                    player.inventory.setItemInMainHand(null)
                    player.sendMessage("§aItem listed successfully!")
                    player.sendMessage("§7Listing fee: ${ItemUtils.formatPrice(fee)} gil")
                    player.sendMessage("§7Expires: ${expiresAt.toLocalDate()}")
                    true
                } else {
                    player.sendMessage("§cFailed to charge listing fee!")
                    false
                }
            } catch (e: Exception) {
                plugin.logger.warning("Failed to list item: ${e.message}")
                player.sendMessage("§cFailed to list item!")
                false
            }
        }
    }
    
    fun placeBid(player: Player, itemId: Int, bidAmount: Long): Boolean {
        return transaction {
            val auctionItem = AuctionItems.select { AuctionItems.id eq itemId and AuctionItems.isActive }
                .singleOrNull()
            
            if (auctionItem == null) {
                player.sendMessage("§cAuction item not found or inactive!")
                return@transaction false
            }
            
            if (auctionItem[AuctionItems.sellerUuid] == player.uniqueId.toString()) {
                player.sendMessage("§cYou cannot bid on your own items!")
                return@transaction false
            }
            
            val currentPrice = auctionItem[AuctionItems.currentPrice]
            val buyoutPrice = auctionItem[AuctionItems.buyoutPrice]
            
            if (bidAmount <= currentPrice) {
                player.sendMessage("§cBid must be higher than current price (${ItemUtils.formatPrice(currentPrice)})!")
                return@transaction false
            }
            
            if (!economy.has(player, bidAmount.toDouble())) {
                player.sendMessage("§cYou don't have enough money!")
                return@transaction false
            }
            
            val isBuyout = buyoutPrice != null && bidAmount >= buyoutPrice
            
            if (isBuyout) {
                completeSale(itemId, player.uniqueId, player.name, buyoutPrice!!)
                player.sendMessage("§aBuyout successful! Item purchased for ${ItemUtils.formatPrice(buyoutPrice)} gil")
            } else {
                Bids.insert {
                    it[Bids.auctionItem] = itemId
                    it[Bids.bidderUuid] = player.uniqueId.toString()
                    it[Bids.bidderName] = player.name
                    it[Bids.bidAmount] = bidAmount
                }
                
                AuctionItems.update({ AuctionItems.id eq itemId }) {
                    it[AuctionItems.currentPrice] = bidAmount
                }
                
                player.sendMessage("§aBid placed successfully! Amount: ${ItemUtils.formatPrice(bidAmount)} gil")
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
                player.sendMessage("§cAuction item not found or you're not the seller!")
                return@transaction false
            }
            
            val hasBids = !Bids.select { Bids.auctionItem eq itemId }.empty()
            if (hasBids) {
                player.sendMessage("§cCannot cancel listing with active bids!")
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
            
            player.sendMessage("§aListing cancelled! Item sent to your mailbox.")
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
                    seller.player?.sendMessage("§aYour item sold for ${ItemUtils.formatPrice(price)} gil!")
                }
            }
            
            AuctionItems.update({ AuctionItems.id eq itemId }) {
                it[AuctionItems.isActive] = false
                it[AuctionItems.isSold] = true
            }
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
                
                true
            } catch (e: Exception) {
                plugin.logger.warning("Failed to cancel auction: ${e.message}")
                player.sendMessage(plugin.langManager.getMessage(player, "general.unknown_error"))
                false
            }
        }
    }
}
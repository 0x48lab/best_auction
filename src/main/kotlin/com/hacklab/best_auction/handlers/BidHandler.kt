package com.hacklab.best_auction.handlers

import com.hacklab.best_auction.Main
import com.hacklab.best_auction.utils.ItemUtils
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class BidHandler(private val plugin: Main) : Listener {
    
    private val activeBids = ConcurrentHashMap<UUID, BidSession>()
    private val activeBuyouts = ConcurrentHashMap<UUID, BuyoutSession>()
    private val activeCancellations = ConcurrentHashMap<UUID, CancellationSession>()
    
    data class BidSession(val itemId: Int, val currentPrice: Long)
    data class BuyoutSession(val itemId: Int, val buyoutPrice: Long)
    data class CancellationSession(val auctionId: Int)
    
    fun startBid(player: Player, itemId: Int, currentPrice: Long) {
        activeBids[player.uniqueId] = BidSession(itemId, currentPrice)
    }
    
    fun startBuyout(player: Player, itemId: Int, buyoutPrice: Long) {
        activeBuyouts[player.uniqueId] = BuyoutSession(itemId, buyoutPrice)
    }
    
    fun startCancellation(player: Player, auctionId: Int) {
        activeCancellations[player.uniqueId] = CancellationSession(auctionId)
    }
    
    fun handleConfirmCommand(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage("§cThis command can only be used by players!")
            return
        }
        
        val player = sender
        plugin.logger.info("Confirm command received from ${player.name}")
        
        when {
            activeCancellations.containsKey(player.uniqueId) -> {
                val session = activeCancellations.remove(player.uniqueId)!!
                plugin.logger.info("Processing cancellation confirmation for ${player.name}, auctionId: ${session.auctionId}")
                plugin.server.scheduler.runTask(plugin, Runnable {
                    val result = plugin.auctionManager.cancelAuction(player, session.auctionId)
                    plugin.logger.info("cancelAuction result for ${player.name}: $result")
                })
            }
            activeBuyouts.containsKey(player.uniqueId) -> {
                val session = activeBuyouts.remove(player.uniqueId)!!
                plugin.logger.info("Processing buyout confirmation for ${player.name}, itemId: ${session.itemId}")
                plugin.server.scheduler.runTask(plugin, Runnable {
                    plugin.auctionManager.placeBid(player, session.itemId, session.buyoutPrice)
                })
            }
            else -> {
                plugin.logger.info("No pending confirmation for ${player.name}")
                player.sendMessage("§c${plugin.langManager.getMessage(player, "general.no_pending_confirmation")}")
            }
        }
    }
    
    @EventHandler
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        val player = event.player
        val message = event.message.trim()
        
        if (activeBids.containsKey(player.uniqueId)) {
            event.isCancelled = true
            handleBidInput(player, message)
        }
        // buyoutとキャンセル処理はコマンドに移行したので削除
    }
    
    private fun handleBidInput(player: Player, input: String) {
        val session = activeBids.remove(player.uniqueId) ?: return
        
        if (input.equals("cancel", ignoreCase = true) || input.equals("キャンセル", ignoreCase = true)) {
            player.sendMessage("§c${plugin.langManager.getMessage(player, "auction.bid_cancelled")}")
            return
        }
        
        // Parse bid amount - allow both direct numbers and formatted text
        val bidAmount = parseAmount(input)
        if (bidAmount == null) {
            player.sendMessage("§c${plugin.langManager.getMessage(player, "auction.invalid_bid_format")}")
            player.sendMessage("§7${plugin.langManager.getMessage(player, "auction.bid_format_example")}: 1000, 1k, 1.5k")
            return
        }
        
        if (bidAmount <= session.currentPrice) {
            player.sendMessage("§c${plugin.langManager.getMessage(player, "auction.bid_too_low", "${ItemUtils.formatPrice(session.currentPrice + 1)}")}")
            return
        }
        
        // Confirmation message
        player.sendMessage("§a${plugin.langManager.getMessage(player, "auction.bid_processing")}")
        player.sendMessage("§7${plugin.langManager.getMessage(player, "auction.bid_amount")}: §e${ItemUtils.formatPrice(bidAmount)} gil")
        
        plugin.server.scheduler.runTask(plugin, Runnable {
            plugin.auctionManager.placeBid(player, session.itemId, bidAmount)
        })
    }
    
    private fun parseAmount(input: String): Long? {
        val cleanInput = input.replace(",", "").replace(" ", "").lowercase()
        
        return when {
            cleanInput.endsWith("k") -> {
                val number = cleanInput.dropLast(1).toDoubleOrNull()
                if (number != null) (number * 1000).toLong() else null
            }
            cleanInput.endsWith("m") -> {
                val number = cleanInput.dropLast(1).toDoubleOrNull()
                if (number != null) (number * 1000000).toLong() else null
            }
            else -> cleanInput.toLongOrNull()
        }
    }
    
    
}
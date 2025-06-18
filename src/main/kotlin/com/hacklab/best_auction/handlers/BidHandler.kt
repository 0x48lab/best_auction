package com.hacklab.best_auction.handlers

import com.hacklab.best_auction.Main
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
    
    @EventHandler
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        val player = event.player
        val message = event.message.trim()
        
        if (activeBids.containsKey(player.uniqueId)) {
            event.isCancelled = true
            handleBidInput(player, message)
        } else if (activeBuyouts.containsKey(player.uniqueId)) {
            event.isCancelled = true
            handleBuyoutInput(player, message)
        } else if (activeCancellations.containsKey(player.uniqueId)) {
            event.isCancelled = true
            handleCancellationInput(player, message)
        }
    }
    
    private fun handleBidInput(player: Player, input: String) {
        val session = activeBids.remove(player.uniqueId) ?: return
        
        if (input.equals("cancel", ignoreCase = true)) {
            player.sendMessage("§cBid cancelled.")
            return
        }
        
        val bidAmount = input.toLongOrNull()
        if (bidAmount == null || bidAmount <= session.currentPrice) {
            player.sendMessage("§cInvalid bid amount! Must be higher than ${session.currentPrice}")
            return
        }
        
        plugin.server.scheduler.runTask(plugin, Runnable {
            plugin.auctionManager.placeBid(player, session.itemId, bidAmount)
        })
    }
    
    private fun handleBuyoutInput(player: Player, input: String) {
        val session = activeBuyouts.remove(player.uniqueId) ?: return
        
        if (input.equals("confirm", ignoreCase = true)) {
            plugin.server.scheduler.runTask(plugin, Runnable {
                plugin.auctionManager.placeBid(player, session.itemId, session.buyoutPrice)
            })
        } else {
            player.sendMessage("§cBuyout cancelled.")
        }
    }
    
    private fun handleCancellationInput(player: Player, input: String) {
        val session = activeCancellations.remove(player.uniqueId) ?: return
        
        if (input.equals("confirm", ignoreCase = true)) {
            plugin.server.scheduler.runTask(plugin, Runnable {
                plugin.auctionManager.cancelAuction(player, session.auctionId)
            })
        } else {
            player.sendMessage(plugin.langManager.getMessage(player, "auction.cancel_cancelled"))
        }
    }
}
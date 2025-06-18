package com.hacklab.best_auction.handlers

import com.hacklab.best_auction.Main
import com.hacklab.best_auction.ui.AuctionUI
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class SearchHandler(private val plugin: Main) : Listener {
    
    private val activeSearches = ConcurrentHashMap<UUID, Boolean>()
    
    fun startSearch(player: Player) {
        activeSearches[player.uniqueId] = true
    }
    
    @EventHandler
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        val player = event.player
        
        if (activeSearches.containsKey(player.uniqueId)) {
            event.isCancelled = true
            activeSearches.remove(player.uniqueId)
            
            val searchTerm = event.message.trim()
            if (searchTerm.isNotEmpty()) {
                plugin.server.scheduler.runTask(plugin, Runnable {
                    AuctionUI.openSearchUI(player, plugin, searchTerm)
                })
            } else {
                player.sendMessage("§cSearch cancelled.")
            }
        }
    }
}
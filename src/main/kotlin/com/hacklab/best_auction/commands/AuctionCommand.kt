package com.hacklab.best_auction.commands

import com.hacklab.best_auction.Main
import com.hacklab.best_auction.ui.AuctionUI
import com.hacklab.best_auction.ui.LanguageSettingsUI
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class AuctionCommand(private val plugin: Main) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(plugin.langManager.getMessage("command.only_players"))
            return true
        }

        when (args.getOrNull(0)?.lowercase()) {
            "sell", "list" -> {
                if (args.size < 2) {
                    plugin.langManager.sendErrorMessage(sender, "command.insufficient_args")
                    return true
                }
                
                val price = args[1].toLongOrNull()
                if (price == null || price <= 0) {
                    plugin.langManager.sendErrorMessage(sender, "auction.invalid_price")
                    return true
                }
                
                val buyoutPrice = args.getOrNull(2)?.toLongOrNull()
                if (buyoutPrice != null && buyoutPrice <= price) {
                    plugin.langManager.sendErrorMessage(sender, "auction.invalid_buyout")
                    return true
                }
                
                plugin.auctionManager.listItem(sender, price, buyoutPrice)
            }
            
            "gui", "open", null -> {
                AuctionUI.openMainUI(sender, plugin)
            }
            
            "bid" -> {
                if (args.size < 3) {
                    plugin.langManager.sendErrorMessage(sender, "command.insufficient_args")
                    return true
                }
                
                val itemId = args[1].toIntOrNull()
                val bidAmount = args[2].toLongOrNull()
                
                if (itemId == null || bidAmount == null || bidAmount <= 0) {
                    plugin.langManager.sendErrorMessage(sender, "command.invalid_number")
                    return true
                }
                
                plugin.auctionManager.placeBid(sender, itemId, bidAmount)
            }
            
            "cancel" -> {
                if (args.size < 2) {
                    plugin.langManager.sendErrorMessage(sender, "command.insufficient_args")
                    return true
                }
                
                val itemId = args[1].toIntOrNull()
                if (itemId == null) {
                    plugin.langManager.sendErrorMessage(sender, "command.invalid_item_id")
                    return true
                }
                
                plugin.auctionManager.cancelListing(sender, itemId)
            }
            
            "mail", "mailbox" -> {
                if (args.size >= 3 && args[1].equals("collect", ignoreCase = true)) {
                    val mailId = args[2].toIntOrNull()
                    if (mailId != null) {
                        plugin.mailManager.collectMail(sender, mailId)
                    } else {
                        plugin.langManager.sendErrorMessage(sender, "command.invalid_item_id")
                    }
                } else {
                    plugin.mailManager.openMailBox(sender)
                }
            }
            
            "search" -> {
                if (args.size < 2) {
                    plugin.langManager.sendErrorMessage(sender, "command.insufficient_args")
                    return true
                }
                
                val searchTerm = args.drop(1).joinToString(" ")
                AuctionUI.openSearchUI(sender, plugin, searchTerm)
            }
            
            "language", "lang" -> {
                LanguageSettingsUI.openLanguageSettings(sender, plugin)
            }
            
            "help" -> {
                sendHelpMessage(sender, label)
            }
            
            else -> {
                sendHelpMessage(sender, label)
            }
        }
        
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (args.size == 1) {
            return listOf("sell", "bid", "cancel", "search", "mail", "language", "help")
                .filter { it.startsWith(args[0].lowercase()) }
        }
        return emptyList()
    }

    private fun sendHelpMessage(player: Player, label: String) {
        plugin.langManager.sendInfoMessage(player, "command.help_header")
        player.sendMessage("§e/$label §7${plugin.langManager.getMessage(player, "command.help_gui")}")
        player.sendMessage("§e/$label sell <price> [buyout] §7${plugin.langManager.getMessage(player, "command.help_sell")}")
        player.sendMessage("§e/$label bid <id> <amount> §7${plugin.langManager.getMessage(player, "command.help_bid")}")
        player.sendMessage("§e/$label cancel <id> §7${plugin.langManager.getMessage(player, "command.help_cancel")}")
        player.sendMessage("§e/$label search <name> §7${plugin.langManager.getMessage(player, "command.help_search")}")
        player.sendMessage("§e/$label mail §7${plugin.langManager.getMessage(player, "command.help_mail")}")
        player.sendMessage("§e/$label language §7- Change language settings")
        player.sendMessage("§e/$label help §7${plugin.langManager.getMessage(player, "command.help_help")}")
    }
}
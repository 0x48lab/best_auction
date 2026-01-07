package com.hacklab.best_auction.commands

import com.hacklab.best_auction.Main
import com.hacklab.best_auction.utils.ItemUtils
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * Shortcut economy commands for internal economy system
 * /balance, /pay, /money
 */
class EconomyCommand(private val plugin: Main, private val commandType: CommandType) : CommandExecutor, TabCompleter {

    enum class CommandType {
        BALANCE,  // /balance, /bal, /money
        PAY       // /pay, /send
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(plugin.langManager.getMessage("command.only_players"))
            return true
        }

        val economyProvider = plugin.getEconomyProvider()
        if (economyProvider == null) {
            plugin.langManager.sendErrorMessage(sender, "general.no_economy")
            return true
        }

        when (commandType) {
            CommandType.BALANCE -> handleBalance(sender, args, economyProvider)
            CommandType.PAY -> handlePay(sender, args, economyProvider)
        }

        return true
    }

    private fun handleBalance(sender: Player, args: Array<out String>, economyProvider: com.hacklab.best_auction.economy.EconomyProvider) {
        val targetName = args.getOrNull(0)
        if (targetName != null && sender.hasPermission("auction.admin")) {
            val target = plugin.server.getOfflinePlayer(targetName)
            if (target.hasPlayedBefore() || target.isOnline) {
                val balance = economyProvider.format(economyProvider.getBalance(target))
                sender.sendMessage(plugin.langManager.getMessage(sender, "economy.balance_other", target.name ?: targetName, balance))
            } else {
                plugin.langManager.sendErrorMessage(sender, "general.player_not_found")
            }
        } else {
            val balance = economyProvider.format(economyProvider.getBalance(sender))
            sender.sendMessage(plugin.langManager.getMessage(sender, "economy.balance_self", balance))
        }
    }

    private fun handlePay(sender: Player, args: Array<out String>, economyProvider: com.hacklab.best_auction.economy.EconomyProvider) {
        if (args.size < 2) {
            plugin.langManager.sendErrorMessage(sender, "command.insufficient_args")
            sender.sendMessage("§7Usage: /pay <player> <amount>")
            return
        }

        val targetName = args[0]
        val amount = ItemUtils.parseAmount(args[1])

        if (amount == null || amount <= 0) {
            plugin.langManager.sendErrorMessage(sender, "economy.pay_invalid_amount")
            return
        }

        val target = plugin.server.getPlayer(targetName)
        if (target == null) {
            plugin.langManager.sendErrorMessage(sender, "general.player_not_found")
            return
        }

        if (target.uniqueId == sender.uniqueId) {
            plugin.langManager.sendErrorMessage(sender, "economy.pay_self")
            return
        }

        if (!economyProvider.has(sender, amount.toDouble())) {
            plugin.langManager.sendErrorMessage(sender, "economy.pay_not_enough")
            return
        }

        if (economyProvider.withdrawPlayer(sender, amount.toDouble()) &&
            economyProvider.depositPlayer(target, amount.toDouble())) {
            val formattedAmount = economyProvider.format(amount.toDouble())
            sender.sendMessage(plugin.langManager.getMessage(sender, "economy.pay_success", target.name, formattedAmount))
            target.sendMessage(plugin.langManager.getMessage(target, "economy.pay_received", sender.name, formattedAmount))
        } else {
            plugin.langManager.sendErrorMessage(sender, "general.unknown_error")
        }
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        when (commandType) {
            CommandType.BALANCE -> {
                if (args.size == 1 && sender.hasPermission("auction.admin")) {
                    return plugin.server.onlinePlayers
                        .map { it.name }
                        .filter { it.lowercase().startsWith(args[0].lowercase()) }
                }
            }
            CommandType.PAY -> {
                if (args.size == 1) {
                    return plugin.server.onlinePlayers
                        .filter { it.name != sender.name }
                        .map { it.name }
                        .filter { it.lowercase().startsWith(args[0].lowercase()) }
                }
            }
        }
        return emptyList()
    }
}

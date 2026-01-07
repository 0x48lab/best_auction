package com.hacklab.best_auction.commands

import com.hacklab.best_auction.Main
import com.hacklab.best_auction.data.AuctionCategory
import com.hacklab.best_auction.ui.AuctionUI
import com.hacklab.best_auction.ui.LanguageSettingsUI
import com.hacklab.best_auction.utils.ItemUtils
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.util.*
import kotlin.random.Random

class AuctionCommand(private val plugin: Main) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        when (args.getOrNull(0)?.lowercase()) {
            "sell", "list" -> {
                if (sender !is Player) {
                    sender.sendMessage(plugin.langManager.getMessage("command.only_players"))
                    return true
                }
                
                if (args.size < 2) {
                    plugin.langManager.sendErrorMessage(sender, "command.insufficient_args")
                    return true
                }
                
                val price = ItemUtils.parseAmount(args[1])
                if (price == null || price <= 0) {
                    plugin.langManager.sendErrorMessage(sender, "auction.invalid_price")
                    return true
                }
                
                val buyoutPrice = args.getOrNull(2)?.let { ItemUtils.parseAmount(it) }
                if (buyoutPrice != null && buyoutPrice <= price) {
                    plugin.langManager.sendErrorMessage(sender, "auction.invalid_buyout")
                    return true
                }
                
                plugin.auctionManager.listItem(sender, price, buyoutPrice)
            }
            
            "gui", "open", null -> {
                if (sender !is Player) {
                    sender.sendMessage(plugin.langManager.getMessage("command.only_players"))
                    return true
                }
                
                AuctionUI.openMainUI(sender, plugin)
            }
            
            "bid" -> {
                if (sender !is Player) {
                    sender.sendMessage(plugin.langManager.getMessage("command.only_players"))
                    return true
                }
                
                if (args.size < 3) {
                    plugin.langManager.sendErrorMessage(sender, "command.insufficient_args")
                    return true
                }
                
                val itemId = args[1].toIntOrNull()
                val bidAmount = ItemUtils.parseAmount(args[2])
                
                if (itemId == null || bidAmount == null || bidAmount <= 0) {
                    plugin.langManager.sendErrorMessage(sender, "command.invalid_number")
                    return true
                }
                
                plugin.auctionManager.placeBid(sender, itemId, bidAmount)
            }
            
            "cancel" -> {
                if (sender !is Player) {
                    sender.sendMessage(plugin.langManager.getMessage("command.only_players"))
                    return true
                }
                
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
                if (sender !is Player) {
                    sender.sendMessage(plugin.langManager.getMessage("command.only_players"))
                    return true
                }
                
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
                if (sender !is Player) {
                    sender.sendMessage(plugin.langManager.getMessage("command.only_players"))
                    return true
                }
                
                if (args.size < 2) {
                    plugin.langManager.sendErrorMessage(sender, "command.insufficient_args")
                    return true
                }
                
                val searchTerm = args.drop(1).joinToString(" ")
                AuctionUI.openSearchUI(sender, plugin, searchTerm)
            }
            
            "language", "lang" -> {
                if (sender !is Player) {
                    sender.sendMessage(plugin.langManager.getMessage("command.only_players"))
                    return true
                }
                
                LanguageSettingsUI.openLanguageSettings(sender, plugin)
            }
            
            "confirm" -> {
                if (sender !is Player) {
                    sender.sendMessage(plugin.langManager.getMessage("command.only_players"))
                    return true
                }
                
                plugin.bidHandler.handleConfirmCommand(sender)
            }
            
            "cloud" -> {
                if (!sender.hasPermission("auction.admin")) {
                    if (sender is Player) {
                        sender.sendMessage(plugin.langManager.getMessage("command.no_permission"))
                    } else {
                        sender.sendMessage(plugin.langManager.getMessage("command.no_permission"))
                    }
                    return true
                }
                
                when (args.getOrNull(1)?.lowercase()) {
                    "sync" -> {
                        val forceFullSync = args.getOrNull(2)?.lowercase() == "force"
                        if (forceFullSync) {
                            sender.sendMessage(plugin.langManager.getMessage("cloud.sync_starting_forced"))
                        } else {
                            sender.sendMessage(plugin.langManager.getMessage("cloud.sync_starting"))
                        }
                        
                        plugin.cloudEventManager.performManualSync(forceFullSync).thenAccept { result ->
                            if (result.success) {
                                sender.sendMessage(plugin.langManager.getMessage("cloud.sync_completed", result.syncedAuctions, result.syncedBids))
                            } else {
                                sender.sendMessage(plugin.langManager.getMessage("cloud.sync_failed", result.errorMessage ?: "Unknown error"))
                            }
                        }
                    }
                    "status" -> {
                        sender.sendMessage(plugin.langManager.getMessage("cloud.status_header"))
                        sender.sendMessage(plugin.langManager.getMessage("cloud.status_enabled", plugin.cloudEventManager.isCloudEnabled()))
                        sender.sendMessage(plugin.langManager.getMessage("cloud.status_token_valid", plugin.cloudEventManager.isTokenValid()))
                        sender.sendMessage(plugin.langManager.getMessage("cloud.status_queue_size", plugin.cloudEventManager.getQueueSize()))
                    }
                    "validate" -> {
                        sender.sendMessage(plugin.langManager.getMessage("cloud.validating_token"))
                        plugin.cloudEventManager.forceTokenValidation().thenAccept { valid ->
                            if (valid) {
                                sender.sendMessage(plugin.langManager.getMessage("cloud.token_validation_success"))
                            } else {
                                sender.sendMessage(plugin.langManager.getMessage("cloud.token_validation_failed"))
                            }
                        }
                    }
                    "gettoken" -> {
                        sender.sendMessage(plugin.langManager.getMessage("cloud.token_url_header"))
                        sender.sendMessage("§b§nhttps://best-auction-cloud.masafumi-t.workers.dev/")
                        sender.sendMessage(plugin.langManager.getMessage("cloud.token_url_instruction"))
                    }
                    "dashboard" -> {
                        val baseUrl = plugin.config.getString("cloud.base_url", "")
                        if (baseUrl.isNullOrBlank()) {
                            sender.sendMessage(plugin.langManager.getMessage("cloud.dashboard_url_not_configured"))
                            return true
                        }
                        sender.sendMessage(plugin.langManager.getMessage("cloud.dashboard_header"))
                        sender.sendMessage("§b§n$baseUrl/dashboard")
                        sender.sendMessage(plugin.langManager.getMessage("cloud.dashboard_instruction"))
                        sender.sendMessage(plugin.langManager.getMessage("cloud.dashboard_server_id", plugin.config.getString("cloud.server_id", "default-server") ?: "default-server"))
                    }
                    "settoken" -> {
                        if (args.size < 3) {
                            sender.sendMessage(plugin.langManager.getMessage("cloud.settoken_usage"))
                            return true
                        }
                        val token = args[2]
                        plugin.config.set("cloud.api-token", token)
                        plugin.saveConfig()
                        sender.sendMessage(plugin.langManager.getMessage("cloud.token_set_validating"))
                        plugin.cloudEventManager.updateToken(token)
                        plugin.cloudEventManager.forceTokenValidation().thenAccept { valid ->
                            if (valid) {
                                sender.sendMessage(plugin.langManager.getMessage("cloud.token_set_success"))
                            } else {
                                sender.sendMessage(plugin.langManager.getMessage("cloud.token_set_failed"))
                            }
                        }
                    }
                    else -> {
                        sender.sendMessage(plugin.langManager.getMessage("cloud.commands_header"))
                        sender.sendMessage(plugin.langManager.getMessage("cloud.command_sync"))
                        sender.sendMessage(plugin.langManager.getMessage("cloud.command_status"))
                        sender.sendMessage(plugin.langManager.getMessage("cloud.command_validate"))
                        sender.sendMessage(plugin.langManager.getMessage("cloud.command_dashboard"))
                        sender.sendMessage(plugin.langManager.getMessage("cloud.command_gettoken"))
                        sender.sendMessage(plugin.langManager.getMessage("cloud.command_settoken"))
                    }
                }
            }
            
            "testdata" -> {
                if (sender !is Player) {
                    sender.sendMessage(plugin.langManager.getMessage("command.only_players"))
                    return true
                }
                
                // Check if debug commands are enabled
                if (!plugin.config.getBoolean("debug.enable_debug_commands", false)) {
                    plugin.langManager.sendErrorMessage(sender, "general.debug_commands_disabled")
                    return true
                }
                
                if (!sender.hasPermission("auction.admin")) {
                    plugin.langManager.sendErrorMessage(sender, "command.no_permission")
                    return true
                }
                
                val count = args.getOrNull(1)?.toIntOrNull() ?: 50
                generateTestData(sender, count)
            }
            
            "balance", "bal", "money" -> {
                if (sender !is Player) {
                    sender.sendMessage(plugin.langManager.getMessage("command.only_players"))
                    return true
                }

                val economyProvider = plugin.getEconomyProvider()
                if (economyProvider == null) {
                    plugin.langManager.sendErrorMessage(sender, "general.no_economy")
                    return true
                }

                val targetName = args.getOrNull(1)
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

            "pay", "send" -> {
                if (sender !is Player) {
                    sender.sendMessage(plugin.langManager.getMessage("command.only_players"))
                    return true
                }

                val economyProvider = plugin.getEconomyProvider()
                if (economyProvider == null) {
                    plugin.langManager.sendErrorMessage(sender, "general.no_economy")
                    return true
                }

                if (args.size < 3) {
                    plugin.langManager.sendErrorMessage(sender, "command.insufficient_args")
                    return true
                }

                val targetName = args[1]
                val amount = ItemUtils.parseAmount(args[2])

                if (amount == null || amount <= 0) {
                    plugin.langManager.sendErrorMessage(sender, "economy.pay_invalid_amount")
                    return true
                }

                val target = plugin.server.getPlayer(targetName)
                if (target == null) {
                    plugin.langManager.sendErrorMessage(sender, "general.player_not_found")
                    return true
                }

                if (target.uniqueId == sender.uniqueId) {
                    plugin.langManager.sendErrorMessage(sender, "economy.pay_self")
                    return true
                }

                if (!economyProvider.has(sender, amount.toDouble())) {
                    plugin.langManager.sendErrorMessage(sender, "economy.pay_not_enough")
                    return true
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

            "eco", "economy" -> {
                if (!sender.hasPermission("auction.admin")) {
                    if (sender is Player) {
                        plugin.langManager.sendErrorMessage(sender, "command.no_permission")
                    } else {
                        sender.sendMessage(plugin.langManager.getMessage("command.no_permission"))
                    }
                    return true
                }

                val internalEconomy = plugin.getInternalEconomy()
                if (internalEconomy == null) {
                    if (sender is Player) {
                        plugin.langManager.sendErrorMessage(sender, "economy.internal_economy_only")
                    } else {
                        sender.sendMessage(plugin.langManager.getMessage("economy.internal_economy_only"))
                    }
                    return true
                }

                if (args.size < 4) {
                    if (sender is Player) {
                        sender.sendMessage(plugin.langManager.getMessage(sender, "economy.eco_usage"))
                    } else {
                        sender.sendMessage(plugin.langManager.getMessage("economy.eco_usage"))
                    }
                    return true
                }

                val action = args[1].lowercase()
                val targetName = args[2]
                val amount = ItemUtils.parseAmount(args[3])

                if (amount == null || amount <= 0) {
                    if (sender is Player) {
                        plugin.langManager.sendErrorMessage(sender, "economy.pay_invalid_amount")
                    } else {
                        sender.sendMessage(plugin.langManager.getMessage("economy.pay_invalid_amount"))
                    }
                    return true
                }

                val target = plugin.server.getOfflinePlayer(targetName)
                if (!target.hasPlayedBefore() && !target.isOnline) {
                    if (sender is Player) {
                        plugin.langManager.sendErrorMessage(sender, "general.player_not_found")
                    } else {
                        sender.sendMessage(plugin.langManager.getMessage("general.player_not_found"))
                    }
                    return true
                }

                val formattedAmount = internalEconomy.format(amount.toDouble())
                val displayName = target.name ?: targetName

                when (action) {
                    "give", "add" -> {
                        internalEconomy.depositPlayer(target, amount.toDouble())
                        val message = if (sender is Player) {
                            plugin.langManager.getMessage(sender, "economy.eco_give_success", displayName, formattedAmount)
                        } else {
                            plugin.langManager.getMessage("economy.eco_give_success", displayName, formattedAmount)
                        }
                        sender.sendMessage(message)
                    }
                    "take", "remove" -> {
                        if (!internalEconomy.has(target, amount.toDouble())) {
                            if (sender is Player) {
                                plugin.langManager.sendErrorMessage(sender, "economy.eco_take_not_enough")
                            } else {
                                sender.sendMessage(plugin.langManager.getMessage("economy.eco_take_not_enough"))
                            }
                            return true
                        }
                        internalEconomy.withdrawPlayer(target, amount.toDouble())
                        val message = if (sender is Player) {
                            plugin.langManager.getMessage(sender, "economy.eco_take_success", displayName, formattedAmount)
                        } else {
                            plugin.langManager.getMessage("economy.eco_take_success", displayName, formattedAmount)
                        }
                        sender.sendMessage(message)
                    }
                    "set" -> {
                        internalEconomy.setBalance(target, amount)
                        val message = if (sender is Player) {
                            plugin.langManager.getMessage(sender, "economy.eco_set_success", displayName, formattedAmount)
                        } else {
                            plugin.langManager.getMessage("economy.eco_set_success", displayName, formattedAmount)
                        }
                        sender.sendMessage(message)
                    }
                    else -> {
                        if (sender is Player) {
                            sender.sendMessage(plugin.langManager.getMessage(sender, "economy.eco_usage"))
                        } else {
                            sender.sendMessage(plugin.langManager.getMessage("economy.eco_usage"))
                        }
                    }
                }
            }

            "help" -> {
                if (sender !is Player) {
                    sendHelpMessageConsole(sender, label)
                } else {
                    sendHelpMessage(sender, label)
                }
            }

            else -> {
                if (sender !is Player) {
                    sendHelpMessageConsole(sender, label)
                } else {
                    sendHelpMessage(sender, label)
                }
            }
        }
        
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        if (args.size == 1) {
            val commands = mutableListOf("sell", "bid", "cancel", "search", "mail", "language", "confirm", "help", "balance", "pay")
            if (sender.hasPermission("auction.admin")) {
                commands.add("cloud")
                commands.add("eco")
                if (plugin.config.getBoolean("debug.enable_debug_commands", false)) {
                    commands.add("testdata")
                }
            }
            return commands.filter { it.startsWith(args[0].lowercase()) }
        }

        if (args.size == 2 && args[0].lowercase() == "cloud") {
            val cloudCommands = listOf("sync", "status", "validate", "dashboard", "gettoken", "settoken")
            return cloudCommands.filter { it.startsWith(args[1].lowercase()) }
        }

        if (args.size == 3 && args[0].lowercase() == "cloud" && args[1].lowercase() == "sync") {
            return listOf("force").filter { it.startsWith(args[2].lowercase()) }
        }

        // Economy command tab completion
        if (args[0].lowercase() in listOf("pay", "send")) {
            if (args.size == 2) {
                return plugin.server.onlinePlayers
                    .filter { it.name != sender.name }
                    .map { it.name }
                    .filter { it.lowercase().startsWith(args[1].lowercase()) }
            }
        }

        if (args[0].lowercase() in listOf("eco", "economy") && sender.hasPermission("auction.admin")) {
            if (args.size == 2) {
                return listOf("give", "take", "set").filter { it.startsWith(args[1].lowercase()) }
            }
            if (args.size == 3) {
                return plugin.server.onlinePlayers
                    .map { it.name }
                    .filter { it.lowercase().startsWith(args[2].lowercase()) }
            }
        }

        if (args[0].lowercase() in listOf("balance", "bal", "money") && sender.hasPermission("auction.admin")) {
            if (args.size == 2) {
                return plugin.server.onlinePlayers
                    .map { it.name }
                    .filter { it.lowercase().startsWith(args[1].lowercase()) }
            }
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
        player.sendMessage("§e/$label confirm §7${plugin.langManager.getMessage(player, "command.help_confirm")}")
        player.sendMessage("§e/$label language §7- Change language settings")
        player.sendMessage("§e/$label balance §7${plugin.langManager.getMessage(player, "economy.help_balance")}")
        player.sendMessage("§e/$label pay <player> <amount> §7${plugin.langManager.getMessage(player, "economy.help_pay")}")
        player.sendMessage("§e/$label help §7${plugin.langManager.getMessage(player, "command.help_help")}")
        if (player.hasPermission("auction.admin")) {
            player.sendMessage("§e/$label eco <give|take|set> <player> <amount> §7${plugin.langManager.getMessage(player, "economy.help_eco")}")
            player.sendMessage("§e/$label cloud §7- Cloud synchronization management")
            player.sendMessage("§e/$label testdata [count] §7- Generate test auction data")
        }
    }
    
    private fun sendHelpMessageConsole(sender: CommandSender, label: String) {
        sender.sendMessage("§6=== Best Auction Help (Console) ===")
        sender.sendMessage("§e/$label cloud §7- Cloud synchronization management")
        sender.sendMessage("§7  /$label cloud sync [force] - Synchronize auction data")
        sender.sendMessage("§7  /$label cloud status - Show cloud status")
        sender.sendMessage("§7  /$label cloud validate - Validate API token")
        sender.sendMessage("§7  /$label cloud gettoken - Get token URL")
        sender.sendMessage("§7  /$label cloud settoken <token> - Set API token")
        sender.sendMessage("§e/$label eco <give|take|set> <player> <amount> §7- Manage player balance (internal economy)")
        sender.sendMessage("§e/$label help §7- Show this help message")
        sender.sendMessage("§7Note: Most auction commands require a player and cannot be used from console.")
    }
    
    private fun generateTestData(player: Player, count: Int) {
        player.sendMessage("§aGenerating $count test auction items...")
        
        val testPlayers = listOf(
            "TestUser1", "TestUser2", "TestUser3", "DemoSeller", "AuctionBot",
            "MinecraftFan", "ItemCollector", "TradeMaster", "ShopKeeper", "CraftExpert"
        )
        
        val testMaterials = listOf(
            Material.DIAMOND_SWORD, Material.DIAMOND_PICKAXE, Material.DIAMOND_AXE,
            Material.IRON_SWORD, Material.IRON_PICKAXE, Material.GOLDEN_APPLE,
            Material.ENCHANTED_GOLDEN_APPLE, Material.NETHERITE_SWORD, Material.NETHERITE_PICKAXE,
            Material.EMERALD, Material.DIAMOND, Material.GOLD_INGOT,
            Material.ANCIENT_DEBRIS, Material.BEACON, Material.ELYTRA,
            Material.TOTEM_OF_UNDYING, Material.DRAGON_EGG, Material.NETHER_STAR,
            Material.SHULKER_BOX, Material.ENDER_CHEST, Material.CHEST,
            Material.CRAFTING_TABLE, Material.FURNACE, Material.BREWING_STAND,
            Material.ANVIL, Material.ENCHANTING_TABLE, Material.BOOKSHELF,
            Material.REDSTONE, Material.REDSTONE_TORCH, Material.PISTON,
            Material.STICKY_PISTON, Material.OBSERVER, Material.HOPPER,
            Material.DISPENSER, Material.DROPPER, Material.COMPARATOR,
            Material.REPEATER, Material.REDSTONE_LAMP, Material.TNT,
            Material.COAL, Material.IRON_INGOT, Material.COPPER_INGOT,
            Material.LAPIS_LAZULI, Material.QUARTZ, Material.AMETHYST_SHARD
        )
        
        repeat(count) {
            val material = testMaterials.random()
            val seller = testPlayers.random()
            val sellerUuid = UUID.nameUUIDFromBytes(seller.toByteArray())
            
            val itemStack = ItemStack(material, Random.nextInt(1, 5))
            val meta = itemStack.itemMeta!!
            
            // Add some variety to item names
            if (Random.nextBoolean()) {
                val adjectives = listOf("Legendary", "Rare", "Epic", "Magical", "Ancient", "Cursed", "Blessed")
                meta.setDisplayName("§6${adjectives.random()} ${material.name.replace("_", " ").lowercase().split(" ").joinToString(" ") { word -> word.replaceFirstChar(Char::uppercase) }}")
            }
            
            itemStack.itemMeta = meta
            
            val basePrice = when {
                material.name.contains("NETHERITE") -> Random.nextLong(5000, 20000)
                material.name.contains("DIAMOND") -> Random.nextLong(1000, 5000)
                material.name.contains("GOLD") -> Random.nextLong(500, 2000)
                material.name.contains("IRON") -> Random.nextLong(100, 1000)
                else -> Random.nextLong(10, 500)
            }
            
            val currentPrice = basePrice + Random.nextLong(0, basePrice / 2)
            val buyoutPrice = if (Random.nextBoolean()) currentPrice + Random.nextLong(currentPrice / 2, currentPrice * 2) else null
            
            val category = AuctionCategory.values().random()
            
            try {
                plugin.auctionManager.createAuctionItem(
                    sellerUuid = sellerUuid,
                    sellerName = seller,
                    itemStack = itemStack,
                    startingPrice = basePrice,
                    currentPrice = currentPrice,
                    buyoutPrice = buyoutPrice,
                    category = category.name
                )
            } catch (e: Exception) {
                plugin.logger.warning("Failed to create test auction item: ${e.message}")
            }
        }
        
        player.sendMessage("§aTest data generation completed! Generated $count auction items.")
        player.sendMessage("§7You can now test the pagination and bidding features.")
    }
}
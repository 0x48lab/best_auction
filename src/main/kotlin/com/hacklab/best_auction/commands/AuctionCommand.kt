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
                        plugin.langManager.sendErrorMessage(sender, "command.no_permission")
                    } else {
                        sender.sendMessage("§cYou don't have permission to use this command.")
                    }
                    return true
                }
                
                when (args.getOrNull(1)?.lowercase()) {
                    "sync" -> {
                        val forceFullSync = args.getOrNull(2)?.lowercase() == "force"
                        val message = if (forceFullSync) "§eForcing full cloud synchronization..." else "§eStarting cloud synchronization..."
                        sender.sendMessage(message)
                        
                        plugin.cloudEventManager.performManualSync(forceFullSync).thenAccept { result ->
                            if (result.success) {
                                sender.sendMessage("§aCloud sync completed! Synced ${result.syncedAuctions} auctions and ${result.syncedBids} bids.")
                            } else {
                                sender.sendMessage("§cCloud sync failed: ${result.errorMessage ?: "Unknown error"}")
                            }
                        }
                    }
                    "status" -> {
                        sender.sendMessage("§e=== Cloud Status ===")
                        sender.sendMessage("§7Enabled: ${plugin.cloudEventManager.isCloudEnabled()}")
                        sender.sendMessage("§7Token Valid: ${plugin.cloudEventManager.isTokenValid()}")
                        sender.sendMessage("§7Queue Size: ${plugin.cloudEventManager.getQueueSize()}")
                        sender.sendMessage("§7Status: ${plugin.cloudEventManager.getSyncStatus()}")
                    }
                    "validate" -> {
                        sender.sendMessage("§eValidating cloud token...")
                        plugin.cloudEventManager.forceTokenValidation().thenAccept { valid ->
                            if (valid) {
                                sender.sendMessage("§aToken validation successful!")
                            } else {
                                sender.sendMessage("§cToken validation failed!")
                            }
                        }
                    }
                    "gettoken" -> {
                        sender.sendMessage("§e=== Get Cloud Token ===")
                        sender.sendMessage("§b§nhttps://best-auction-cloud.masafumi-t.workers.dev/")
                        sender.sendMessage("§7Visit the URL above to get your API token.")
                    }
                    "settoken" -> {
                        if (args.size < 3) {
                            sender.sendMessage("§cUsage: /auction cloud settoken <token>")
                            return true
                        }
                        val token = args[2]
                        plugin.config.set("cloud.api-token", token)
                        plugin.saveConfig()
                        sender.sendMessage("§eToken set. Validating...")
                        plugin.cloudEventManager.updateToken(token)
                        plugin.cloudEventManager.forceTokenValidation().thenAccept { valid ->
                            if (valid) {
                                sender.sendMessage("§aToken set and validated successfully!")
                            } else {
                                sender.sendMessage("§cToken set but validation failed!")
                            }
                        }
                    }
                    else -> {
                        sender.sendMessage("§e=== Cloud Commands ===")
                        sender.sendMessage("§7/auction cloud sync [force] - Synchronize auction data")
                        sender.sendMessage("§7/auction cloud status - Show cloud status")
                        sender.sendMessage("§7/auction cloud validate - Validate API token")
                        sender.sendMessage("§7/auction cloud gettoken - Get token URL")
                        sender.sendMessage("§7/auction cloud settoken <token> - Set API token")
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
            val commands = mutableListOf("sell", "bid", "cancel", "search", "mail", "language", "confirm", "help")
            if (sender.hasPermission("auction.admin")) {
                commands.add("cloud")
                if (plugin.config.getBoolean("debug.enable_debug_commands", false)) {
                    commands.add("testdata")
                }
            }
            return commands.filter { it.startsWith(args[0].lowercase()) }
        }
        
        if (args.size == 2 && args[0].lowercase() == "cloud") {
            val cloudCommands = listOf("sync", "status", "validate", "gettoken", "settoken")
            return cloudCommands.filter { it.startsWith(args[1].lowercase()) }
        }
        
        if (args.size == 3 && args[0].lowercase() == "cloud" && args[1].lowercase() == "sync") {
            return listOf("force").filter { it.startsWith(args[2].lowercase()) }
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
        player.sendMessage("§e/$label help §7${plugin.langManager.getMessage(player, "command.help_help")}")
        if (player.hasPermission("auction.admin")) {
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
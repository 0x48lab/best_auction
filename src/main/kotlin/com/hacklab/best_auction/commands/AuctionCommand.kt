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
                AuctionUI.openMainUI(sender, plugin)
            }
            
            "bid" -> {
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
            
            "confirm" -> {
                plugin.bidHandler.handleConfirmCommand(sender)
            }
            
            "cloud" -> {
                if (!sender.hasPermission("auction.admin")) {
                    plugin.langManager.sendErrorMessage(sender, "command.no_permission")
                    return true
                }
                
                when (args.getOrNull(1)?.lowercase()) {
                    "sync" -> {
                        val forceFullSync = args.getOrNull(2)?.lowercase() == "force"
                        val messageKey = if (forceFullSync) "cloud.sync_starting_forced" else "cloud.sync_starting"
                        plugin.langManager.sendInfoMessage(sender, messageKey)
                        
                        plugin.cloudEventManager.performManualSync(forceFullSync).thenAccept { result ->
                            if (result.success) {
                                val message = plugin.langManager.getMessage(sender, "cloud.sync_completed", result.syncedAuctions.toString(), result.syncedBids.toString())
                                sender.sendMessage("§a$message")
                            } else {
                                val message = plugin.langManager.getMessage(sender, "cloud.sync_failed", result.errorMessage ?: "Unknown error")
                                sender.sendMessage("§c$message")
                            }
                        }
                    }
                    "status" -> {
                        plugin.langManager.sendInfoMessage(sender, "cloud.status_header")
                        val enabledMsg = plugin.langManager.getMessage(sender, "cloud.status_enabled", plugin.cloudEventManager.isCloudEnabled().toString())
                        val tokenValidMsg = plugin.langManager.getMessage(sender, "cloud.status_token_valid", plugin.cloudEventManager.isTokenValid().toString())
                        val queueSizeMsg = plugin.langManager.getMessage(sender, "cloud.status_queue_size", plugin.cloudEventManager.getQueueSize().toString())
                        sender.sendMessage("§7$enabledMsg")
                        sender.sendMessage("§7$tokenValidMsg")
                        sender.sendMessage("§7$queueSizeMsg")
                        sender.sendMessage("§7- ${plugin.cloudEventManager.getSyncStatus()}")
                    }
                    "validate" -> {
                        plugin.langManager.sendInfoMessage(sender, "cloud.validating_token")
                        plugin.cloudEventManager.forceTokenValidation().thenAccept { valid ->
                            if (valid) {
                                plugin.langManager.sendSuccessMessage(sender, "cloud.token_validation_success")
                            } else {
                                plugin.langManager.sendErrorMessage(sender, "cloud.token_validation_failed")
                            }
                        }
                    }
                    "gettoken" -> {
                        plugin.langManager.sendInfoMessage(sender, "cloud.token_url_header")
                        sender.sendMessage("§b§nhttps://best-auction-cloud.masafumi-t.workers.dev/")
                        plugin.langManager.sendInfoMessage(sender, "cloud.token_url_instruction")
                    }
                    "settoken" -> {
                        if (args.size < 3) {
                            plugin.langManager.sendErrorMessage(sender, "cloud.settoken_usage")
                            return true
                        }
                        val token = args[2]
                        plugin.config.set("cloud.api-token", token)
                        plugin.saveConfig()
                        plugin.langManager.sendInfoMessage(sender, "cloud.token_set_validating")
                        plugin.cloudEventManager.updateToken(token)
                        plugin.cloudEventManager.forceTokenValidation().thenAccept { valid ->
                            if (valid) {
                                plugin.langManager.sendSuccessMessage(sender, "cloud.token_set_success")
                            } else {
                                plugin.langManager.sendErrorMessage(sender, "cloud.token_set_failed")
                            }
                        }
                    }
                    else -> {
                        plugin.langManager.sendInfoMessage(sender, "cloud.commands_header")
                        val syncMsg = plugin.langManager.getMessage(sender, "cloud.command_sync")
                        val statusMsg = plugin.langManager.getMessage(sender, "cloud.command_status")
                        val validateMsg = plugin.langManager.getMessage(sender, "cloud.command_validate")
                        val gettokenMsg = plugin.langManager.getMessage(sender, "cloud.command_gettoken")
                        val settokenMsg = plugin.langManager.getMessage(sender, "cloud.command_settoken")
                        sender.sendMessage("§7$syncMsg")
                        sender.sendMessage("§7$statusMsg")
                        sender.sendMessage("§7$validateMsg")
                        sender.sendMessage("§7$gettokenMsg")
                        sender.sendMessage("§7$settokenMsg")
                    }
                }
            }
            
            "testdata" -> {
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
                sendHelpMessage(sender, label)
            }
            
            else -> {
                sendHelpMessage(sender, label)
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
            player.sendMessage("§e/$label testdata [count] §7- Generate test auction data")
        }
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
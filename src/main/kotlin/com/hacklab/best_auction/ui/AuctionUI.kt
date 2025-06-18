package com.hacklab.best_auction.ui

import com.hacklab.best_auction.Main
import com.hacklab.best_auction.data.AuctionCategory
import com.hacklab.best_auction.data.AuctionItem
import com.hacklab.best_auction.utils.ItemUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

class AuctionUI : Listener {

    companion object {
        private const val MAIN_TITLE = "§6Auction House"
        private const val CATEGORY_TITLE = "§6Category: "
        private const val SEARCH_TITLE = "§6Search Results"
        
        fun openMainUI(player: Player, plugin: Main) {
            val inventory = Bukkit.createInventory(null, 54, plugin.langManager.getMessage(player, "ui.auction_house"))
            
            AuctionCategory.values().forEachIndexed { index, category ->
                if (index < 45) {
                    val item = createCategoryItem(category, plugin, player)
                    inventory.setItem(index, item)
                }
            }
            
            val searchItem = ItemStack(Material.SPYGLASS)
            val searchMeta = searchItem.itemMeta!!
            searchMeta.setDisplayName("§e" + plugin.langManager.getMessage(player, "ui.search"))
            searchMeta.lore = listOf("§7" + plugin.langManager.getMessage(player, "ui.click_to_search"))
            searchItem.itemMeta = searchMeta
            inventory.setItem(45, searchItem)
            
            val mailItem = ItemStack(Material.CHEST)
            val mailMeta = mailItem.itemMeta!!
            mailMeta.setDisplayName("§e" + plugin.langManager.getMessage(player, "ui.mailbox"))
            mailMeta.lore = listOf("§7" + plugin.langManager.getMessage(player, "ui.click_to_open_mail"))
            mailItem.itemMeta = mailMeta
            inventory.setItem(46, mailItem)
            
            val myListingsItem = ItemStack(Material.LECTERN)
            val myListingsMeta = myListingsItem.itemMeta!!
            myListingsMeta.setDisplayName("§e" + plugin.langManager.getMessage(player, "ui.your_auctions"))
            myListingsMeta.lore = listOf("§7" + plugin.langManager.getMessage(player, "ui.click_to_view_listings"))
            myListingsItem.itemMeta = myListingsMeta
            inventory.setItem(47, myListingsItem)
            
            // Language settings button
            val langItem = ItemStack(Material.WRITABLE_BOOK)
            val langMeta = langItem.itemMeta!!
            langMeta.setDisplayName("§e" + plugin.langManager.getMessage(player, "ui.settings"))
            langMeta.lore = listOf("§7" + plugin.langManager.getMessage(player, "ui.click_to_settings"))
            langItem.itemMeta = langMeta
            inventory.setItem(48, langItem)
            
            player.openInventory(inventory)
        }
        
        fun openCategoryUI(player: Player, plugin: Main, category: AuctionCategory) {
            val categoryDisplayName = getCategoryDisplayName(category, plugin, player)
            val inventory = Bukkit.createInventory(null, 54, "§6$categoryDisplayName")
            val items = plugin.auctionManager.getActiveListings(category.name)
            
            items.take(45).forEachIndexed { index, auctionItem ->
                val displayItem = createAuctionDisplayItem(auctionItem, player, plugin)
                inventory.setItem(index, displayItem)
            }
            
            val backItem = ItemStack(Material.ARROW)
            val backMeta = backItem.itemMeta!!
            backMeta.setDisplayName("§c" + plugin.langManager.getMessage(player, "ui.back"))
            backItem.itemMeta = backMeta
            inventory.setItem(53, backItem)
            
            player.openInventory(inventory)
        }
        
        fun openSearchUI(player: Player, plugin: Main, searchTerm: String) {
            val searchTitle = plugin.langManager.getMessage(player, "ui.search_results")
            val inventory = Bukkit.createInventory(null, 54, "§6$searchTitle: $searchTerm")
            val items = plugin.auctionManager.getActiveListings(searchTerm = searchTerm)
            
            items.take(45).forEachIndexed { index, auctionItem ->
                val displayItem = createAuctionDisplayItem(auctionItem, player, plugin)
                inventory.setItem(index, displayItem)
            }
            
            val backItem = ItemStack(Material.ARROW)
            val backMeta = backItem.itemMeta!!
            backMeta.setDisplayName("§c" + plugin.langManager.getMessage(player, "ui.back"))
            backItem.itemMeta = backMeta
            inventory.setItem(53, backItem)
            
            player.openInventory(inventory)
        }
        
        private fun createCategoryItem(category: AuctionCategory, plugin: Main, player: Player): ItemStack {
            val item = ItemStack(category.material)
            val meta = item.itemMeta!!
            meta.setDisplayName("§e${getCategoryDisplayName(category, plugin, player)}")
            meta.lore = listOf(
                "§7" + plugin.langManager.getMessage(player, "ui.click_to_browse"),
                "§7" + plugin.langManager.getMessage(player, "ui.category_items_available")
            )
            item.itemMeta = meta
            return item
        }
        
        private fun getCategoryDisplayName(category: AuctionCategory, plugin: Main, player: Player): String {
            return when (category) {
                AuctionCategory.BUILDING_BLOCKS -> plugin.langManager.getMessage(player, "category.blocks")
                AuctionCategory.DECORATIONS -> plugin.langManager.getMessage(player, "category.decorations")
                AuctionCategory.REDSTONE -> plugin.langManager.getMessage(player, "category.redstone")
                AuctionCategory.TRANSPORTATION -> plugin.langManager.getMessage(player, "category.transportation")
                AuctionCategory.MISCELLANEOUS -> plugin.langManager.getMessage(player, "category.misc")
                AuctionCategory.FOOD -> plugin.langManager.getMessage(player, "category.food")
                AuctionCategory.TOOLS -> plugin.langManager.getMessage(player, "category.tools")
                AuctionCategory.COMBAT -> plugin.langManager.getMessage(player, "category.weapons")
                AuctionCategory.BREWING -> plugin.langManager.getMessage(player, "category.brewing")
            }
        }
        
        private fun createAuctionDisplayItem(auctionItem: AuctionItem, player: Player, plugin: Main): ItemStack {
            val displayItem = auctionItem.itemStack.clone()
            val originalMeta = auctionItem.itemStack.itemMeta!!
            val meta = displayItem.itemMeta!!
            
            // アイテムの正式名前（Material名から生成）
            val officialName = displayItem.type.name.replace("_", " ").lowercase()
                .split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
            
            // 表示名（元のアイテムのカスタム名があればそれを使用、なければ正式名）
            val displayName = if (originalMeta.hasDisplayName()) {
                originalMeta.displayName!!
            } else {
                officialName
            }
            
            meta.setDisplayName("§6$displayName")
            
            val lore = mutableListOf<String>()
            lore.add("§7アイテム正式名: §f$officialName")
            if (originalMeta.hasDisplayName()) {
                lore.add("§7設定名: §f${originalMeta.displayName}")
            }
            lore.add("§7${plugin.langManager.getMessage(player, "ui.seller")}: §f${auctionItem.sellerName}")
            lore.add("§7${plugin.langManager.getMessage(player, "ui.current_bid")}: §a${ItemUtils.formatPrice(auctionItem.currentPrice)} gil")
            
            if (auctionItem.buyoutPrice != null) {
                lore.add("§7${plugin.langManager.getMessage(player, "ui.buyout_price")}: §e${ItemUtils.formatPrice(auctionItem.buyoutPrice)} gil")
            }
            
            lore.add("§7数量: §f${auctionItem.quantity}")
            lore.add("§7期限: §f${auctionItem.expiresAt.toLocalDate()}")
            lore.add("")
            // Check if this is the player's own item
            if (auctionItem.sellerUuid == player.uniqueId) {
                lore.add("§c${plugin.langManager.getMessage(player, "ui.click_to_cancel")}")
                lore.add("§7${plugin.langManager.getMessage(player, "ui.cancel_warning")}")
            } else {
                lore.add("§e${plugin.langManager.getMessage(player, "ui.click_to_bid")}")
                if (auctionItem.buyoutPrice != null) {
                    lore.add("§e${plugin.langManager.getMessage(player, "ui.click_to_buyout")}")
                }
            }
            lore.add("")
            lore.add("§8ID: ${auctionItem.id}")
            
            meta.lore = lore
            displayItem.itemMeta = meta
            
            return displayItem
        }
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val title = event.view.title
        val plugin = Main.instance
        
        // Check if this is one of our auction UIs
        val auctionHouseTitle = plugin.langManager.getMessage(player, "ui.auction_house")
        val searchResultsTitle = plugin.langManager.getMessage(player, "ui.search_results")
        val yourAuctionsTitle = plugin.langManager.getMessage(player, "ui.your_auctions")
        val mailboxTitle = plugin.langManager.getMessage(player, "ui.mailbox")
        
        // Check if it's a category page by looking for category names
        val isCategoryPage = AuctionCategory.values().any { category -> 
            title.contains(getCategoryDisplayName(category, plugin, player))
        }
        
        val isAuctionUI = title.contains(auctionHouseTitle) ||
                         title.startsWith(CATEGORY_TITLE) ||
                         title.startsWith(SEARCH_TITLE) ||
                         title.contains(searchResultsTitle) ||
                         title.contains(yourAuctionsTitle) ||
                         title.contains(mailboxTitle) ||
                         title.contains("Auction") ||
                         isCategoryPage
        
        if (!isAuctionUI) return
        
        event.isCancelled = true
        
        val clickedItem = event.currentItem ?: return
        
        when {
            title.contains(auctionHouseTitle) -> handleMainMenuClick(player, clickedItem, plugin)
            title.startsWith(CATEGORY_TITLE) -> handleCategoryClick(player, clickedItem, plugin, event.isRightClick)
            title.startsWith(SEARCH_TITLE) -> handleCategoryClick(player, clickedItem, plugin, event.isRightClick)
            title.contains(searchResultsTitle) -> handleCategoryClick(player, clickedItem, plugin, event.isRightClick)
            title.contains(yourAuctionsTitle) -> handleMyListingsClick(player, clickedItem, plugin)
            title.contains(mailboxTitle) -> handleMailBoxClick(player, clickedItem, plugin)
            isCategoryPage -> handleCategoryClick(player, clickedItem, plugin, event.isRightClick)
        }
    }
    
    private fun handleMainMenuClick(player: Player, clickedItem: ItemStack, plugin: Main) {
        when (clickedItem.type) {
            Material.SPYGLASS -> {
                player.closeInventory()
                plugin.langManager.sendInfoMessage(player, "ui.type_search_term")
                plugin.searchHandler.startSearch(player)
            }
            Material.CHEST -> {
                player.closeInventory()
                plugin.mailManager.openMailBox(player)
            }
            Material.LECTERN -> {
                openMyListingsUI(player, plugin)
            }
            Material.WRITABLE_BOOK -> {
                player.closeInventory()
                LanguageSettingsUI.openLanguageSettings(player, plugin)
            }
            else -> {
                // Find category by material type instead of display name
                val category = AuctionCategory.values().find { it.material == clickedItem.type }
                if (category != null) {
                    openCategoryUI(player, plugin, category)
                }
            }
        }
    }
    
    private fun handleCategoryClick(player: Player, clickedItem: ItemStack, plugin: Main, isRightClick: Boolean) {
        if (clickedItem.type == Material.ARROW) {
            openMainUI(player, plugin)
            return
        }
        
        val meta = clickedItem.itemMeta ?: return
        val lore = meta.lore ?: return
        
        val sellerLine = lore.find { it.startsWith("§7${plugin.langManager.getMessage(player, "ui.seller")}:") } ?: return
        val seller = sellerLine.replace("§7${plugin.langManager.getMessage(player, "ui.seller")}: §f", "")
        
        val auctionId = findAuctionItemId(lore)
        
        if (seller == player.name) {
            // This is the player's own item - allow cancellation
            player.closeInventory()
            plugin.logger.info("Starting cancellation for player ${player.name}, auctionId: $auctionId")
            player.sendMessage("§c${plugin.langManager.getMessage(player, "ui.confirm_cancel")}")
            player.sendMessage("§7${plugin.langManager.getMessage(player, "ui.type_confirm_cancel")}")
            
            // Start cancellation confirmation
            plugin.bidHandler.startCancellation(player, auctionId)
            return
        }
        
        val priceLine = lore.find { it.startsWith("§7Current Price:") } ?: return
        val currentPrice = extractPrice(priceLine)
        
        val buyoutLine = lore.find { it.startsWith("§7Buyout Price:") }
        val buyoutPrice = buyoutLine?.let { extractPrice(it) }
        
        player.closeInventory()
        
        if (isRightClick && buyoutPrice != null) {
            player.sendMessage("§eBuyout price: ${ItemUtils.formatPrice(buyoutPrice)} gil")
            player.sendMessage("§e'/ah confirm' コマンドで購入を実行してください。")
            plugin.bidHandler.startBuyout(player, auctionId, buyoutPrice)
            plugin.logger.info("Started buyout session for player ${player.name}, itemId: $auctionId, price: $buyoutPrice")
        } else {
            player.sendMessage("§eCurrent price: ${ItemUtils.formatPrice(currentPrice)} gil")
            player.sendMessage("§eEnter your bid amount:")
            plugin.bidHandler.startBid(player, auctionId, currentPrice)
            plugin.logger.info("Started bid session for player ${player.name}, itemId: $auctionId, currentPrice: $currentPrice")
        }
    }
    
    private fun openMyListingsUI(player: Player, plugin: Main) {
        val title = plugin.langManager.getMessage(player, "ui.your_auctions")
        val inventory = Bukkit.createInventory(null, 54, "§6$title")
        val myItems = plugin.auctionManager.getPlayerListings(player.uniqueId)
        
        myItems.take(45).forEachIndexed { index, auctionItem ->
            val displayItem = createAuctionDisplayItem(auctionItem, player, plugin)
            inventory.setItem(index, displayItem)
        }
        
        val backItem = ItemStack(Material.ARROW)
        val backMeta = backItem.itemMeta!!
        backMeta.setDisplayName("§c" + plugin.langManager.getMessage(player, "ui.back"))
        backItem.itemMeta = backMeta
        inventory.setItem(53, backItem)
        
        player.openInventory(inventory)
    }
    
    private fun handleMyListingsClick(player: Player, clickedItem: ItemStack, plugin: Main) {
        if (clickedItem.type == Material.ARROW) {
            openMainUI(player, plugin)
            return
        }
        
        val meta = clickedItem.itemMeta ?: return
        val lore = meta.lore ?: return
        
        // Extract auction ID from lore
        val auctionId = findAuctionItemId(lore)
        
        // Check if it's the player's own listing (from seller line)
        val sellerLine = lore.find { it.startsWith("§7${plugin.langManager.getMessage(player, "ui.seller")}:") }
        if (sellerLine != null) {
            val seller = sellerLine.replace("§7${plugin.langManager.getMessage(player, "ui.seller")}: §f", "")
            
            if (seller == player.name) {
                // This is the player's own item - allow cancellation
                player.closeInventory()
                plugin.logger.info("Starting cancellation from My Listings for player ${player.name}, auctionId: $auctionId")
                player.sendMessage("§c${plugin.langManager.getMessage(player, "ui.confirm_cancel")}")
                player.sendMessage("§7${plugin.langManager.getMessage(player, "ui.type_confirm_cancel")}")
                
                // Start cancellation confirmation
                plugin.bidHandler.startCancellation(player, auctionId)
                return
            }
        }
    }
    
    private fun extractPrice(line: String): Long {
        return line.replace(Regex("[^0-9.]"), "").replace(".", "").toLongOrNull() ?: 0L
    }
    
    private fun handleMailBoxClick(player: Player, clickedItem: ItemStack, plugin: Main) {
        if (plugin.mailManager.handleMailBoxClick(player, clickedItem)) {
            // Back button was clicked - return to main menu
            openMainUI(player, plugin)
        }
    }
    
    private fun findAuctionItemId(lore: List<String>): Int {
        val idLine = lore.find { it.startsWith("§8ID: ") }
        return idLine?.replace("§8ID: ", "")?.toIntOrNull() ?: 0
    }
}
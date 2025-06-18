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
        private const val ITEMS_PER_PAGE = 45
        
        // Session data to track current page and search parameters
        private val playerPages = mutableMapOf<String, Int>()
        private val playerSessions = mutableMapOf<String, PaginationSession>()
        
        data class PaginationSession(
            val type: SessionType,
            val category: AuctionCategory? = null,
            val searchTerm: String? = null
        )
        
        enum class SessionType {
            CATEGORY, SEARCH, MY_LISTINGS
        }
        
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
            // My bids button  
            val myBidsItem = ItemStack(Material.GOLDEN_SWORD)
            val myBidsMeta = myBidsItem.itemMeta!!
            myBidsMeta.setDisplayName("§e" + plugin.langManager.getMessage(player, "ui.my_bids"))
            myBidsMeta.lore = listOf("§7" + plugin.langManager.getMessage(player, "ui.click_to_view_bids"))
            myBidsItem.itemMeta = myBidsMeta
            inventory.setItem(48, myBidsItem)
            
            val langItem = ItemStack(Material.WRITABLE_BOOK)
            val langMeta = langItem.itemMeta!!
            langMeta.setDisplayName("§e" + plugin.langManager.getMessage(player, "ui.settings"))
            langMeta.lore = listOf("§7" + plugin.langManager.getMessage(player, "ui.click_to_settings"))
            langItem.itemMeta = langMeta
            inventory.setItem(49, langItem)
            
            player.openInventory(inventory)
        }
        
        fun openCategoryUI(player: Player, plugin: Main, category: AuctionCategory, page: Int = 0) {
            val categoryDisplayName = getCategoryDisplayName(category, plugin, player)
            val inventory = Bukkit.createInventory(null, 54, "§6$categoryDisplayName")
            val items = plugin.auctionManager.getActiveListings(category.name)
            
            // Store session data
            playerPages[player.name] = page
            playerSessions[player.name] = PaginationSession(SessionType.CATEGORY, category)
            
            val startIndex = page * ITEMS_PER_PAGE
            val endIndex = minOf(startIndex + ITEMS_PER_PAGE, items.size)
            
            items.subList(startIndex, endIndex).forEachIndexed { index, auctionItem ->
                val displayItem = createAuctionDisplayItem(auctionItem, player, plugin)
                inventory.setItem(index, displayItem)
            }
            
            // Add navigation buttons  
            addNavigationButtons(inventory, plugin, player, page, items.size)
            
            player.openInventory(inventory)
        }
        
        fun openSearchUI(player: Player, plugin: Main, searchTerm: String, page: Int = 0) {
            val searchTitle = plugin.langManager.getMessage(player, "ui.search_results")
            val inventory = Bukkit.createInventory(null, 54, "§6$searchTitle: $searchTerm")
            val items = plugin.auctionManager.getActiveListings(searchTerm = searchTerm)
            
            // Store session data
            playerPages[player.name] = page
            playerSessions[player.name] = PaginationSession(SessionType.SEARCH, searchTerm = searchTerm)
            
            val startIndex = page * ITEMS_PER_PAGE
            val endIndex = minOf(startIndex + ITEMS_PER_PAGE, items.size)
            
            items.subList(startIndex, endIndex).forEachIndexed { index, auctionItem ->
                val displayItem = createAuctionDisplayItem(auctionItem, player, plugin)
                inventory.setItem(index, displayItem)
            }
            
            // Add navigation buttons
            addNavigationButtons(inventory, plugin, player, page, items.size)
            
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
            lore.add("§7${plugin.langManager.getMessage(player, "ui.current_bid")}: §a${ItemUtils.formatPriceWithCurrency(auctionItem.currentPrice, plugin.getEconomy(), plugin)}")
            
            if (auctionItem.buyoutPrice != null) {
                lore.add("§7${plugin.langManager.getMessage(player, "ui.buyout_price")}: §e${ItemUtils.formatPriceWithCurrency(auctionItem.buyoutPrice, plugin.getEconomy(), plugin)}")
            }
            
            lore.add("§7数量: §f${auctionItem.quantity}")
            lore.add("§7期限: §f${ItemUtils.formatDate(auctionItem.expiresAt, plugin)}")
            val timeRemaining = ItemUtils.formatTimeRemaining(auctionItem.expiresAt, plugin.langManager, player)
            lore.add("§7${plugin.langManager.getMessage(player, "time.remaining", timeRemaining)}")
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
        
        fun openMyBidsUI(player: Player, plugin: Main, page: Int = 0) {
            val title = plugin.langManager.getMessage(player, "ui.my_bids")
            val inventory = Bukkit.createInventory(null, 54, "§6$title")
            val myBids = plugin.auctionManager.getPlayerBids(player.uniqueId)
            
            // Store session data
            playerPages[player.name] = page
            playerSessions[player.name] = PaginationSession(SessionType.MY_LISTINGS) // Reuse MY_LISTINGS type for pagination
            
            val startIndex = page * ITEMS_PER_PAGE
            val endIndex = minOf(startIndex + ITEMS_PER_PAGE, myBids.size)
            
            myBids.subList(startIndex, endIndex).forEachIndexed { index, auctionItem ->
                val displayItem = createBidDisplayItem(auctionItem, player, plugin)
                inventory.setItem(index, displayItem)
            }
            
            // Add navigation buttons
            addNavigationButtons(inventory, plugin, player, page, myBids.size)
            
            player.openInventory(inventory)
        }
        
        private fun createBidDisplayItem(auctionItem: AuctionItem, player: Player, plugin: Main): ItemStack {
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
            lore.add("§7${plugin.langManager.getMessage(player, "ui.current_bid")}: §a${ItemUtils.formatPriceWithCurrency(auctionItem.currentPrice, plugin.getEconomy(), plugin)}")
            
            // Show player's bid amount
            if (auctionItem.playerBidAmount != null) {
                lore.add("§7${plugin.langManager.getMessage(player, "ui.your_bid")}: §e${ItemUtils.formatPriceWithCurrency(auctionItem.playerBidAmount, plugin.getEconomy(), plugin)}")
                
                // Show if player is winning or losing
                if (auctionItem.playerBidAmount == auctionItem.currentPrice) {
                    lore.add("§a${plugin.langManager.getMessage(player, "ui.winning_bid")}")
                } else {
                    lore.add("§c${plugin.langManager.getMessage(player, "ui.outbid")}")
                }
            }
            
            if (auctionItem.buyoutPrice != null) {
                lore.add("§7${plugin.langManager.getMessage(player, "ui.buyout_price")}: §e${ItemUtils.formatPriceWithCurrency(auctionItem.buyoutPrice, plugin.getEconomy(), plugin)}")
            }
            
            lore.add("§7数量: §f${auctionItem.quantity}")
            lore.add("§7期限: §f${ItemUtils.formatDate(auctionItem.expiresAt, plugin)}")
            val timeRemaining = ItemUtils.formatTimeRemaining(auctionItem.expiresAt, plugin.langManager, player)
            lore.add("§7${plugin.langManager.getMessage(player, "time.remaining", timeRemaining)}")
            lore.add("")
            lore.add("§e${plugin.langManager.getMessage(player, "ui.click_to_change_bid")}")
            lore.add("§c${plugin.langManager.getMessage(player, "ui.right_click_to_cancel_bid")}")
            lore.add("§7${plugin.langManager.getMessage(player, "ui.cancel_bid_warning")}")
            lore.add("")
            lore.add("§8ID: ${auctionItem.id}")
            
            meta.lore = lore
            displayItem.itemMeta = meta
            
            return displayItem
        }

        private fun addNavigationButtons(inventory: Inventory, plugin: Main, player: Player, currentPage: Int, totalItems: Int) {
            val totalPages = (totalItems + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE
            
            // Previous page button
            if (currentPage > 0) {
                val prevItem = ItemStack(Material.SPECTRAL_ARROW)
                val prevMeta = prevItem.itemMeta!!
                prevMeta.setDisplayName("§e« §a" + plugin.langManager.getMessage(player, "ui.previous_page"))
                prevMeta.lore = listOf(
                    "§7" + plugin.langManager.getMessage(player, "ui.page_info", "${currentPage + 1}", "$totalPages"),
                    "§7← クリックで前のページへ"
                )
                prevItem.itemMeta = prevMeta
                inventory.setItem(48, prevItem)
            }
            
            // Page info
            val pageInfoItem = ItemStack(Material.BOOK)
            val pageInfoMeta = pageInfoItem.itemMeta!!
            pageInfoMeta.setDisplayName("§e" + plugin.langManager.getMessage(player, "ui.page_indicator"))
            pageInfoMeta.lore = listOf(
                "§7" + plugin.langManager.getMessage(player, "ui.current_page", "${currentPage + 1}"),
                "§7" + plugin.langManager.getMessage(player, "ui.total_pages", "$totalPages"),
                "§7" + plugin.langManager.getMessage(player, "ui.total_items", "$totalItems")
            )
            pageInfoItem.itemMeta = pageInfoMeta
            inventory.setItem(49, pageInfoItem)
            
            // Next page button
            if (currentPage < totalPages - 1) {
                val nextItem = ItemStack(Material.TIPPED_ARROW)
                val nextMeta = nextItem.itemMeta!!
                nextMeta.setDisplayName("§a" + plugin.langManager.getMessage(player, "ui.next_page") + " §e»")
                nextMeta.lore = listOf(
                    "§7" + plugin.langManager.getMessage(player, "ui.page_info", "${currentPage + 1}", "$totalPages"),
                    "§7クリックで次のページへ →"
                )
                nextItem.itemMeta = nextMeta
                inventory.setItem(50, nextItem)
            }
            
            // Back to main menu button
            val backItem = ItemStack(Material.ARROW)
            val backMeta = backItem.itemMeta!!
            backMeta.setDisplayName("§c" + plugin.langManager.getMessage(player, "ui.back"))
            backItem.itemMeta = backMeta
            inventory.setItem(53, backItem)
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
        val myBidsTitle = plugin.langManager.getMessage(player, "ui.my_bids")
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
                         title.contains(myBidsTitle) ||
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
            title.contains(myBidsTitle) -> handleMyBidsClick(player, clickedItem, plugin, event.isRightClick)
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
            Material.GOLDEN_SWORD -> {
                openMyBidsUI(player, plugin)
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
        
        // Handle pagination buttons
        if (clickedItem.type == Material.SPECTRAL_ARROW || clickedItem.type == Material.TIPPED_ARROW) {
            val displayName = clickedItem.itemMeta?.displayName ?: ""
            val currentPage = playerPages[player.name] ?: 0
            val session = playerSessions[player.name] ?: return
            
            when {
                displayName.contains(plugin.langManager.getMessage(player, "ui.previous_page")) -> {
                    when (session.type) {
                        SessionType.CATEGORY -> session.category?.let { openCategoryUI(player, plugin, it, currentPage - 1) }
                        SessionType.SEARCH -> session.searchTerm?.let { openSearchUI(player, plugin, it, currentPage - 1) }
                        SessionType.MY_LISTINGS -> openMyListingsUI(player, plugin, currentPage - 1)
                    }
                    return
                }
                displayName.contains(plugin.langManager.getMessage(player, "ui.next_page")) -> {
                    when (session.type) {
                        SessionType.CATEGORY -> session.category?.let { openCategoryUI(player, plugin, it, currentPage + 1) }
                        SessionType.SEARCH -> session.searchTerm?.let { openSearchUI(player, plugin, it, currentPage + 1) }
                        SessionType.MY_LISTINGS -> openMyListingsUI(player, plugin, currentPage + 1)
                    }
                    return
                }
            }
        }
        
        // Handle page info button (no action)
        if (clickedItem.type == Material.BOOK && clickedItem.itemMeta?.displayName?.contains(plugin.langManager.getMessage(player, "ui.page_indicator")) == true) {
            return
        }
        
        val meta = clickedItem.itemMeta ?: return
        val lore = meta.lore ?: return
        
        // Debug: Print lore contents
        plugin.logger.info("=== AUCTION ITEM CLICK DEBUG ===")
        plugin.logger.info("Player: ${player.name}")
        plugin.logger.info("Item: ${clickedItem.type}")
        plugin.logger.info("Lore contents:")
        lore.forEachIndexed { index, line ->
            plugin.logger.info("  [$index] $line")
        }
        
        val auctionId = findAuctionItemId(lore)
        
        if (auctionId <= 0) {
            plugin.logger.warning("Invalid auction ID found: $auctionId")
            player.sendMessage("§cエラー: オークションIDが見つかりません")
            return
        }
        
        player.closeInventory()
        
        // Get fresh auction data from database instead of relying on lore
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val auctionInfo = plugin.auctionManager.getAuctionInfo(auctionId)
            
            plugin.server.scheduler.runTask(plugin, Runnable {
                if (auctionInfo == null) {
                    player.sendMessage("§c${plugin.langManager.getMessage(player, "auction.item_not_found")}")
                    return@Runnable
                }
                
                // Check if this is the player's own item
                if (auctionInfo.sellerUuid == player.uniqueId) {
                    plugin.logger.info("Starting cancellation for player ${player.name}, auctionId: $auctionId")
                    player.sendMessage("§c${plugin.langManager.getMessage(player, "ui.confirm_cancel")}")
                    player.sendMessage("§7${plugin.langManager.getMessage(player, "ui.type_confirm_cancel")}")
                    plugin.bidHandler.startCancellation(player, auctionId)
                    return@Runnable
                }
                
                val currentPrice = auctionInfo.currentPrice
                val buyoutPrice = auctionInfo.buyoutPrice
        
                if (isRightClick && buyoutPrice != null) {
                    player.sendMessage("§eBuyout price: ${ItemUtils.formatPriceWithCurrency(buyoutPrice, plugin.getEconomy(), plugin)}")
                    player.sendMessage("§e'/ah confirm' コマンドで購入を実行してください。")
                    plugin.bidHandler.startBuyout(player, auctionId, buyoutPrice)
                    plugin.logger.info("Started buyout session for player ${player.name}, itemId: $auctionId, price: $buyoutPrice")
                } else {
                    val timeRemaining = ItemUtils.formatTimeRemaining(auctionInfo.expiresAt, plugin.langManager, player)
                    
                    player.sendMessage("§e━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    player.sendMessage("§6${plugin.langManager.getMessage(player, "ui.bid_prompt_header")}")
                    player.sendMessage("§7${plugin.langManager.getMessage(player, "ui.current_highest_bid")}: §a${ItemUtils.formatPriceWithCurrency(currentPrice, plugin.getEconomy(), plugin)}")
                    player.sendMessage("§7${plugin.langManager.getMessage(player, "ui.minimum_bid")}: §e${ItemUtils.formatPriceWithCurrency(currentPrice + 1, plugin.getEconomy(), plugin)}")
                    if (buyoutPrice != null) {
                        player.sendMessage("§7${plugin.langManager.getMessage(player, "ui.buyout_price")}: §6${ItemUtils.formatPriceWithCurrency(buyoutPrice, plugin.getEconomy(), plugin)}")
                        player.sendMessage("§8${plugin.langManager.getMessage(player, "ui.buyout_hint")}")
                    }
                    player.sendMessage("§7${plugin.langManager.getMessage(player, "time.remaining", timeRemaining)}")
                    player.sendMessage("")
                    player.sendMessage("§e${plugin.langManager.getMessage(player, "ui.enter_bid_amount")}:")
                    player.sendMessage("§8${plugin.langManager.getMessage(player, "ui.bid_command_hint")}: §7/ah bid $auctionId <金額>")
                    player.sendMessage("§e━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    plugin.bidHandler.startBid(player, auctionId, currentPrice)
                    plugin.logger.info("Started bid session for player ${player.name}, itemId: $auctionId, currentPrice: $currentPrice")
                }
            })
        })
    }
    
    private fun openMyListingsUI(player: Player, plugin: Main, page: Int = 0) {
        val title = plugin.langManager.getMessage(player, "ui.your_auctions")
        val inventory = Bukkit.createInventory(null, 54, "§6$title")
        val myItems = plugin.auctionManager.getPlayerListings(player.uniqueId)
        
        // Store session data
        playerPages[player.name] = page
        playerSessions[player.name] = PaginationSession(SessionType.MY_LISTINGS)
        
        val startIndex = page * ITEMS_PER_PAGE
        val endIndex = minOf(startIndex + ITEMS_PER_PAGE, myItems.size)
        
        myItems.subList(startIndex, endIndex).forEachIndexed { index, auctionItem ->
            val displayItem = createAuctionDisplayItem(auctionItem, player, plugin)
            inventory.setItem(index, displayItem)
        }
        
        // Add navigation buttons
        addNavigationButtons(inventory, plugin, player, page, myItems.size)
        
        player.openInventory(inventory)
    }
    
    private fun handleMyListingsClick(player: Player, clickedItem: ItemStack, plugin: Main) {
        if (clickedItem.type == Material.ARROW) {
            openMainUI(player, plugin)
            return
        }
        
        // Handle pagination buttons
        if (clickedItem.type == Material.SPECTRAL_ARROW || clickedItem.type == Material.TIPPED_ARROW) {
            val displayName = clickedItem.itemMeta?.displayName ?: ""
            val currentPage = playerPages[player.name] ?: 0
            
            when {
                displayName.contains(plugin.langManager.getMessage(player, "ui.previous_page")) -> {
                    openMyListingsUI(player, plugin, currentPage - 1)
                    return
                }
                displayName.contains(plugin.langManager.getMessage(player, "ui.next_page")) -> {
                    openMyListingsUI(player, plugin, currentPage + 1)
                    return
                }
            }
        }
        
        // Handle page info button (no action)
        if (clickedItem.type == Material.BOOK && clickedItem.itemMeta?.displayName?.contains(plugin.langManager.getMessage(player, "ui.page_indicator")) == true) {
            return
        }
        
        val meta = clickedItem.itemMeta ?: return
        val lore = meta.lore ?: return
        
        // Extract auction ID from lore
        val auctionId = findAuctionItemId(lore)
        
        if (auctionId <= 0) {
            plugin.logger.warning("Invalid auction ID found in my listings: $auctionId")
            return
        }
        
        // Verify ownership through database instead of lore
        player.closeInventory()
        
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val auctionInfo = plugin.auctionManager.getAuctionInfo(auctionId)
            
            plugin.server.scheduler.runTask(plugin, Runnable {
                if (auctionInfo == null) {
                    player.sendMessage("§c${plugin.langManager.getMessage(player, "auction.item_not_found")}")
                    return@Runnable
                }
                
                // Check if this is the player's own listing
                if (auctionInfo.sellerUuid == player.uniqueId) {
                    plugin.logger.info("Starting cancellation from My Listings for player ${player.name}, auctionId: $auctionId")
                    player.sendMessage("§c${plugin.langManager.getMessage(player, "ui.confirm_cancel")}")
                    player.sendMessage("§7${plugin.langManager.getMessage(player, "ui.type_confirm_cancel")}")
                    plugin.bidHandler.startCancellation(player, auctionId)
                } else {
                    player.sendMessage("§c${plugin.langManager.getMessage(player, "auction.not_your_auction")}")
                }
            })
        })
    }
    
    private fun handleMyBidsClick(player: Player, clickedItem: ItemStack, plugin: Main, isRightClick: Boolean = false) {
        if (clickedItem.type == Material.ARROW) {
            openMainUI(player, plugin)
            return
        }
        
        // Handle pagination buttons
        if (clickedItem.type == Material.SPECTRAL_ARROW || clickedItem.type == Material.TIPPED_ARROW) {
            val displayName = clickedItem.itemMeta?.displayName ?: ""
            val currentPage = playerPages[player.name] ?: 0
            
            when {
                displayName.contains(plugin.langManager.getMessage(player, "ui.previous_page")) -> {
                    openMyBidsUI(player, plugin, currentPage - 1)
                    return
                }
                displayName.contains(plugin.langManager.getMessage(player, "ui.next_page")) -> {
                    openMyBidsUI(player, plugin, currentPage + 1)
                    return
                }
            }
        }
        
        // Handle page info button (no action)
        if (clickedItem.type == Material.BOOK && clickedItem.itemMeta?.displayName?.contains(plugin.langManager.getMessage(player, "ui.page_indicator")) == true) {
            return
        }
        
        val meta = clickedItem.itemMeta ?: return
        val lore = meta.lore ?: return
        
        // Extract auction ID from lore
        val auctionId = findAuctionItemId(lore)
        
        if (auctionId <= 0) {
            plugin.logger.warning("Invalid auction ID found in my bids click: $auctionId")
            return
        }
        
        player.closeInventory()
        
        if (isRightClick) {
            // Right click - Cancel the bid
            player.sendMessage("§e${plugin.langManager.getMessage(player, "auction.cancelling_bid")}...")
            
            plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
                val success = plugin.auctionManager.cancelPlayerBid(player, auctionId)
                
                plugin.server.scheduler.runTask(plugin, Runnable {
                    if (success) {
                        // Refresh the my bids UI
                        openMyBidsUI(player, plugin)
                    } else {
                        // If failed, just reopen the UI
                        openMyBidsUI(player, plugin)
                    }
                })
            })
        } else {
            // Left click - Change bid amount
            // Get current auction info to show current price
            plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
                val auctionInfo = plugin.auctionManager.getAuctionInfo(auctionId, player.uniqueId)
                
                plugin.server.scheduler.runTask(plugin, Runnable {
                    if (auctionInfo != null) {
                        val currentPrice = auctionInfo.currentPrice
                        val playerBidAmount = auctionInfo.playerBidAmount ?: 0L
                        val timeRemaining = ItemUtils.formatTimeRemaining(auctionInfo.expiresAt, plugin.langManager, player)
                        
                        player.sendMessage("§e━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                        player.sendMessage("§6${plugin.langManager.getMessage(player, "ui.change_bid_header")}")
                        player.sendMessage("§7${plugin.langManager.getMessage(player, "ui.current_highest_bid")}: §a${ItemUtils.formatPriceWithCurrency(currentPrice, plugin.getEconomy(), plugin)}")
                        player.sendMessage("§7${plugin.langManager.getMessage(player, "ui.your_current_bid")}: §e${ItemUtils.formatPriceWithCurrency(playerBidAmount, plugin.getEconomy(), plugin)}")
                        player.sendMessage("§7${plugin.langManager.getMessage(player, "ui.minimum_bid")}: §e${ItemUtils.formatPriceWithCurrency(currentPrice + 1, plugin.getEconomy(), plugin)}")
                        if (auctionInfo.buyoutPrice != null) {
                            player.sendMessage("§7${plugin.langManager.getMessage(player, "ui.buyout_price")}: §6${ItemUtils.formatPriceWithCurrency(auctionInfo.buyoutPrice, plugin.getEconomy(), plugin)}")
                            player.sendMessage("§8${plugin.langManager.getMessage(player, "ui.buyout_hint")}")
                        }
                        player.sendMessage("§7${plugin.langManager.getMessage(player, "time.remaining", timeRemaining)}")
                        player.sendMessage("")
                        player.sendMessage("§e${plugin.langManager.getMessage(player, "ui.enter_new_bid_amount")}:")
                        player.sendMessage("§8${plugin.langManager.getMessage(player, "ui.bid_command_hint")}: §7/ah bid $auctionId <金額>")
                        player.sendMessage("§e━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                        
                        plugin.bidHandler.startBid(player, auctionId, currentPrice)
                    } else {
                        player.sendMessage("§c${plugin.langManager.getMessage(player, "auction.item_not_found")}")
                        openMyBidsUI(player, plugin)
                    }
                })
            })
        }
    }
    
    
    private fun handleMailBoxClick(player: Player, clickedItem: ItemStack, plugin: Main) {
        if (plugin.mailManager.handleMailBoxClick(player, clickedItem)) {
            // Back button was clicked - return to main menu
            openMainUI(player, plugin)
        }
    }
    
    private fun findAuctionItemId(lore: List<String>): Int {
        return try {
            val idLine = lore.find { it.startsWith("§8ID: ") }
            val idString = idLine?.replace("§8ID: ", "")?.trim()
            idString?.toIntOrNull() ?: 0
        } catch (e: Exception) {
            0
        }
    }
}
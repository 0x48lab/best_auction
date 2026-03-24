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
        private const val SEARCH_TITLE = "§6Search Results"
        private const val ITEMS_PER_PAGE = 36

        // Session data to track current page and search parameters
        private val playerPages = mutableMapOf<String, Int>()
        private val playerSessions = mutableMapOf<String, PaginationSession>()

        data class PaginationSession(
            val type: SessionType,
            val category: AuctionCategory = AuctionCategory.ALL,
            val searchTerm: String? = null
        )

        enum class SessionType {
            MAIN, SEARCH, MY_LISTINGS
        }

        fun openMainUI(player: Player, plugin: Main, category: AuctionCategory = AuctionCategory.ALL, page: Int = 0) {
            val categoryDisplayName = getCategoryDisplayName(category, plugin, player)
            val title = "${plugin.langManager.getMessage(player, "ui.auction_house")} - $categoryDisplayName"
            val inventory = Bukkit.createInventory(null, 54, "§6$title")
            val items = plugin.auctionManager.getActiveListings(category)

            // Store session data
            playerPages[player.name] = page
            playerSessions[player.name] = PaginationSession(SessionType.MAIN, category)

            // === Row 1 (slots 0-8): カテゴリフィルタ ===
            AuctionCategory.values().forEachIndexed { index, cat ->
                val item = createCategoryFilterItem(cat, plugin, player, cat == category)
                inventory.setItem(index, item)
            }

            // === Row 2-5 (slots 9-44): アイテム一覧 ===
            val startIndex = page * ITEMS_PER_PAGE
            val endIndex = minOf(startIndex + ITEMS_PER_PAGE, items.size)

            items.subList(startIndex, endIndex).forEachIndexed { index, auctionItem ->
                val displayItem = createAuctionDisplayItem(auctionItem, player, plugin)
                inventory.setItem(9 + index, displayItem)
            }

            // === Row 6 (slots 45-53): 個人メニュー + ユーティリティ + ページ送り ===
            // Personal: slots 45-47 (left group)
            val myListingsItem = ItemStack(Material.LECTERN)
            val myListingsMeta = myListingsItem.itemMeta!!
            myListingsMeta.setDisplayName("§e" + plugin.langManager.getMessage(player, "ui.your_auctions"))
            myListingsMeta.lore = listOf("§7" + plugin.langManager.getMessage(player, "ui.click_to_view_listings"))
            myListingsItem.itemMeta = myListingsMeta
            inventory.setItem(45, myListingsItem)

            val myBidsItem = ItemStack(Material.GOLDEN_SWORD)
            val myBidsMeta = myBidsItem.itemMeta!!
            myBidsMeta.setDisplayName("§e" + plugin.langManager.getMessage(player, "ui.my_bids"))
            myBidsMeta.lore = listOf("§7" + plugin.langManager.getMessage(player, "ui.click_to_view_bids"))
            myBidsItem.itemMeta = myBidsMeta
            inventory.setItem(46, myBidsItem)

            val mailItem = ItemStack(Material.ENDER_CHEST)
            val mailMeta = mailItem.itemMeta!!
            mailMeta.setDisplayName("§e" + plugin.langManager.getMessage(player, "ui.mailbox"))
            mailMeta.lore = listOf("§7" + plugin.langManager.getMessage(player, "ui.click_to_open_mail"))
            mailItem.itemMeta = mailMeta
            inventory.setItem(47, mailItem)

            // Utility: slots 49-50 (center group)
            val searchItem = ItemStack(Material.SPYGLASS)
            val searchMeta = searchItem.itemMeta!!
            searchMeta.setDisplayName("§e" + plugin.langManager.getMessage(player, "ui.search"))
            searchMeta.lore = listOf("§7" + plugin.langManager.getMessage(player, "ui.click_to_search"))
            searchItem.itemMeta = searchMeta
            inventory.setItem(49, searchItem)

            val settingsItem = ItemStack(Material.WRITABLE_BOOK)
            val settingsMeta = settingsItem.itemMeta!!
            settingsMeta.setDisplayName("§e" + plugin.langManager.getMessage(player, "ui.settings"))
            settingsMeta.lore = listOf("§7" + plugin.langManager.getMessage(player, "ui.click_to_settings"))
            settingsItem.itemMeta = settingsMeta
            inventory.setItem(50, settingsItem)

            // Pagination: slots 51-53 (right group)
            addMainPaginationButtons(inventory, plugin, player, page, items.size)

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

            // Add navigation buttons (sub-page style: pagination + back)
            addSubPageNavigationButtons(inventory, plugin, player, page, items.size)

            player.openInventory(inventory)
        }

        private fun createCategoryFilterItem(category: AuctionCategory, plugin: Main, player: Player, isSelected: Boolean): ItemStack {
            val item = ItemStack(category.material)
            val meta = item.itemMeta!!
            val displayName = getCategoryDisplayName(category, plugin, player)
            meta.setDisplayName(if (isSelected) "§a§l$displayName" else "§e$displayName")
            meta.lore = listOf(
                "§7" + plugin.langManager.getMessage(player, "ui.click_to_browse")
            )
            if (isSelected) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true)
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
            }
            item.itemMeta = meta
            return item
        }

        fun getCategoryDisplayName(category: AuctionCategory, plugin: Main, player: Player): String {
            return when (category) {
                AuctionCategory.ALL -> plugin.langManager.getMessage(player, "category.all")
                AuctionCategory.BLOCKS -> plugin.langManager.getMessage(player, "category.blocks")
                AuctionCategory.FOOD -> plugin.langManager.getMessage(player, "category.food")
                AuctionCategory.WEAPONS_EQUIPMENT -> plugin.langManager.getMessage(player, "category.weapons_equipment")
                AuctionCategory.ENCHANTMENTS -> plugin.langManager.getMessage(player, "category.enchantments")
                AuctionCategory.OTHERS -> plugin.langManager.getMessage(player, "category.others")
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
            lore.add("§7${plugin.langManager.getMessage(player, "ui.current_bid")}: §a${ItemUtils.formatPriceWithCurrency(auctionItem.currentPrice, plugin.getEconomyProvider(), plugin)}")
            
            if (auctionItem.buyoutPrice != null) {
                lore.add("§7${plugin.langManager.getMessage(player, "ui.buyout_price")}: §e${ItemUtils.formatPriceWithCurrency(auctionItem.buyoutPrice, plugin.getEconomyProvider(), plugin)}")
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
            playerSessions[player.name] = PaginationSession(SessionType.MY_LISTINGS)

            val startIndex = page * ITEMS_PER_PAGE
            val endIndex = minOf(startIndex + ITEMS_PER_PAGE, myBids.size)

            myBids.subList(startIndex, endIndex).forEachIndexed { index, auctionItem ->
                val displayItem = createBidDisplayItem(auctionItem, player, plugin)
                inventory.setItem(index, displayItem)
            }

            addSubPageNavigationButtons(inventory, plugin, player, page, myBids.size)

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
            lore.add("§7${plugin.langManager.getMessage(player, "ui.current_bid")}: §a${ItemUtils.formatPriceWithCurrency(auctionItem.currentPrice, plugin.getEconomyProvider(), plugin)}")
            
            // Show player's bid amount
            if (auctionItem.playerBidAmount != null) {
                lore.add("§7${plugin.langManager.getMessage(player, "ui.your_bid")}: §e${ItemUtils.formatPriceWithCurrency(auctionItem.playerBidAmount, plugin.getEconomyProvider(), plugin)}")
                
                // Show if player is winning or losing
                if (auctionItem.playerBidAmount == auctionItem.currentPrice) {
                    lore.add("§a${plugin.langManager.getMessage(player, "ui.winning_bid")}")
                } else {
                    lore.add("§c${plugin.langManager.getMessage(player, "ui.outbid")}")
                }
            }
            
            if (auctionItem.buyoutPrice != null) {
                lore.add("§7${plugin.langManager.getMessage(player, "ui.buyout_price")}: §e${ItemUtils.formatPriceWithCurrency(auctionItem.buyoutPrice, plugin.getEconomyProvider(), plugin)}")
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

        private fun addMainPaginationButtons(inventory: Inventory, plugin: Main, player: Player, currentPage: Int, totalItems: Int) {
            val totalPages = maxOf((totalItems + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE, 1)

            // Previous page (slot 51)
            if (currentPage > 0) {
                val prevItem = ItemStack(Material.SPECTRAL_ARROW)
                val prevMeta = prevItem.itemMeta!!
                prevMeta.setDisplayName("§e« §a" + plugin.langManager.getMessage(player, "ui.previous_page"))
                prevMeta.lore = listOf(
                    "§7" + plugin.langManager.getMessage(player, "ui.page_info", "${currentPage + 1}", "$totalPages")
                )
                prevItem.itemMeta = prevMeta
                inventory.setItem(51, prevItem)
            }

            // Page info (slot 52)
            val pageInfoItem = ItemStack(Material.BOOK)
            val pageInfoMeta = pageInfoItem.itemMeta!!
            pageInfoMeta.setDisplayName("§e" + plugin.langManager.getMessage(player, "ui.page_indicator"))
            pageInfoMeta.lore = listOf(
                "§7" + plugin.langManager.getMessage(player, "ui.current_page", "${currentPage + 1}"),
                "§7" + plugin.langManager.getMessage(player, "ui.total_pages", "$totalPages"),
                "§7" + plugin.langManager.getMessage(player, "ui.total_items", "$totalItems")
            )
            pageInfoItem.itemMeta = pageInfoMeta
            inventory.setItem(52, pageInfoItem)

            // Next page (slot 53)
            if (currentPage < totalPages - 1) {
                val nextItem = ItemStack(Material.TIPPED_ARROW)
                val nextMeta = nextItem.itemMeta!!
                nextMeta.setDisplayName("§a" + plugin.langManager.getMessage(player, "ui.next_page") + " §e»")
                nextMeta.lore = listOf(
                    "§7" + plugin.langManager.getMessage(player, "ui.page_info", "${currentPage + 1}", "$totalPages")
                )
                nextItem.itemMeta = nextMeta
                inventory.setItem(53, nextItem)
            }
        }

        private fun addSubPageNavigationButtons(inventory: Inventory, plugin: Main, player: Player, currentPage: Int, totalItems: Int) {
            val totalPages = maxOf((totalItems + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE, 1)

            // Previous page (slot 48)
            if (currentPage > 0) {
                val prevItem = ItemStack(Material.SPECTRAL_ARROW)
                val prevMeta = prevItem.itemMeta!!
                prevMeta.setDisplayName("§e« §a" + plugin.langManager.getMessage(player, "ui.previous_page"))
                prevMeta.lore = listOf(
                    "§7" + plugin.langManager.getMessage(player, "ui.page_info", "${currentPage + 1}", "$totalPages")
                )
                prevItem.itemMeta = prevMeta
                inventory.setItem(48, prevItem)
            }

            // Page info (slot 49)
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

            // Next page (slot 50)
            if (currentPage < totalPages - 1) {
                val nextItem = ItemStack(Material.TIPPED_ARROW)
                val nextMeta = nextItem.itemMeta!!
                nextMeta.setDisplayName("§a" + plugin.langManager.getMessage(player, "ui.next_page") + " §e»")
                nextMeta.lore = listOf(
                    "§7" + plugin.langManager.getMessage(player, "ui.page_info", "${currentPage + 1}", "$totalPages")
                )
                nextItem.itemMeta = nextMeta
                inventory.setItem(50, nextItem)
            }

            // Back button (slot 53)
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

        val isAuctionUI = title.contains(auctionHouseTitle) ||
                         title.startsWith(SEARCH_TITLE) ||
                         title.contains(searchResultsTitle) ||
                         title.contains(yourAuctionsTitle) ||
                         title.contains(myBidsTitle) ||
                         title.contains(mailboxTitle)

        if (!isAuctionUI) return

        event.isCancelled = true

        val clickedItem = event.currentItem ?: return

        when {
            title.contains(auctionHouseTitle) -> handleMainMenuClick(player, event.rawSlot, clickedItem, plugin, event.isRightClick)
            title.startsWith(SEARCH_TITLE) || title.contains(searchResultsTitle) -> handleSubPageClick(player, event.rawSlot, clickedItem, plugin, event.isRightClick)
            title.contains(yourAuctionsTitle) -> handleSubPageClick(player, event.rawSlot, clickedItem, plugin, event.isRightClick, isMyListings = true)
            title.contains(myBidsTitle) -> handleMyBidsClick(player, event.rawSlot, clickedItem, plugin, event.isRightClick)
            title.contains(mailboxTitle) -> handleMailBoxClick(player, clickedItem, plugin)
        }
    }

    private fun handleMainMenuClick(player: Player, slot: Int, clickedItem: ItemStack, plugin: Main, isRightClick: Boolean) {
        when (slot) {
            // Row 1: Category filters (slots 0-5)
            in 0..5 -> {
                val categories = AuctionCategory.values()
                if (slot < categories.size) {
                    openMainUI(player, plugin, categories[slot])
                }
            }
            // Row 2-5: Item area (slots 9-44)
            in 9..44 -> handleAuctionItemClick(player, clickedItem, plugin, isRightClick)
            // Row 6: Personal - My Listings (slot 45)
            45 -> openMyListingsUI(player, plugin)
            // Row 6: Personal - My Bids (slot 46)
            46 -> openMyBidsUI(player, plugin)
            // Row 6: Personal - Mailbox (slot 47)
            47 -> {
                player.closeInventory()
                plugin.mailManager.openMailBox(player)
            }
            // Row 6: Utility - Search (slot 49)
            49 -> {
                player.closeInventory()
                plugin.langManager.sendInfoMessage(player, "ui.type_search_term")
                plugin.searchHandler.startSearch(player)
            }
            // Row 6: Utility - Settings (slot 50)
            50 -> {
                player.closeInventory()
                LanguageSettingsUI.openLanguageSettings(player, plugin)
            }
            // Row 6: Pagination (slots 51-53)
            51, 53 -> handlePaginationClick(player, clickedItem, plugin)
            52 -> return // Page info - no action
        }
    }

    private fun handlePaginationClick(player: Player, clickedItem: ItemStack, plugin: Main) {
        val displayName = clickedItem.itemMeta?.displayName ?: ""
        val currentPage = playerPages[player.name] ?: 0
        val session = playerSessions[player.name] ?: return

        val newPage = when {
            displayName.contains(plugin.langManager.getMessage(player, "ui.previous_page")) -> currentPage - 1
            displayName.contains(plugin.langManager.getMessage(player, "ui.next_page")) -> currentPage + 1
            else -> return
        }

        when (session.type) {
            SessionType.MAIN -> openMainUI(player, plugin, session.category, newPage)
            SessionType.SEARCH -> session.searchTerm?.let { openSearchUI(player, plugin, it, newPage) }
            SessionType.MY_LISTINGS -> openMyListingsUI(player, plugin, newPage)
        }
    }
    
    private fun handleSubPageClick(player: Player, slot: Int, clickedItem: ItemStack, plugin: Main, isRightClick: Boolean, isMyListings: Boolean = false) {
        when (slot) {
            in 0..35 -> {
                if (isMyListings) {
                    handleMyListingsItemClick(player, clickedItem, plugin)
                } else {
                    handleAuctionItemClick(player, clickedItem, plugin, isRightClick)
                }
            }
            48, 50 -> handlePaginationClick(player, clickedItem, plugin) // Prev/Next
            49 -> return // Page info
            53 -> openMainUI(player, plugin) // Back
        }
    }

    private fun handleAuctionItemClick(player: Player, clickedItem: ItemStack, plugin: Main, isRightClick: Boolean) {
        val meta = clickedItem.itemMeta ?: return
        val lore = meta.lore ?: return

        val auctionId = findAuctionItemId(lore)

        if (auctionId <= 0) {
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
                    player.sendMessage("§c${plugin.langManager.getMessage(player, "ui.confirm_cancel")}")
                    player.sendMessage("§7${plugin.langManager.getMessage(player, "ui.type_confirm_cancel")}")
                    plugin.bidHandler.startCancellation(player, auctionId)
                    return@Runnable
                }

                val currentPrice = auctionInfo.currentPrice
                val buyoutPrice = auctionInfo.buyoutPrice

                if (isRightClick && buyoutPrice != null) {
                    player.sendMessage("§eBuyout price: ${ItemUtils.formatPriceWithCurrency(buyoutPrice, plugin.getEconomyProvider(), plugin)}")
                    player.sendMessage("§e'/ah confirm' コマンドで購入を実行してください。")
                    plugin.bidHandler.startBuyout(player, auctionId, buyoutPrice)
                } else {
                    val timeRemaining = ItemUtils.formatTimeRemaining(auctionInfo.expiresAt, plugin.langManager, player)

                    player.sendMessage("§e━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    player.sendMessage("§6${plugin.langManager.getMessage(player, "ui.bid_prompt_header")}")
                    player.sendMessage("§7${plugin.langManager.getMessage(player, "ui.current_highest_bid")}: §a${ItemUtils.formatPriceWithCurrency(currentPrice, plugin.getEconomyProvider(), plugin)}")
                    player.sendMessage("§7${plugin.langManager.getMessage(player, "ui.minimum_bid")}: §e${ItemUtils.formatPriceWithCurrency(currentPrice + 1, plugin.getEconomyProvider(), plugin)}")
                    if (buyoutPrice != null) {
                        player.sendMessage("§7${plugin.langManager.getMessage(player, "ui.buyout_price")}: §6${ItemUtils.formatPriceWithCurrency(buyoutPrice, plugin.getEconomyProvider(), plugin)}")
                        player.sendMessage("§8${plugin.langManager.getMessage(player, "ui.buyout_hint")}")
                    }
                    player.sendMessage("§7${plugin.langManager.getMessage(player, "time.remaining", timeRemaining)}")
                    player.sendMessage("")
                    player.sendMessage("§e${plugin.langManager.getMessage(player, "ui.enter_bid_amount")}:")
                    player.sendMessage("§8${plugin.langManager.getMessage(player, "ui.bid_command_hint")}: §7/ah bid $auctionId <金額>")
                    player.sendMessage("§e━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    plugin.bidHandler.startBid(player, auctionId, currentPrice)
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

        addSubPageNavigationButtons(inventory, plugin, player, page, myItems.size)

        player.openInventory(inventory)
    }
    
    private fun handleMyListingsItemClick(player: Player, clickedItem: ItemStack, plugin: Main) {
        val meta = clickedItem.itemMeta ?: return
        val lore = meta.lore ?: return
        val auctionId = findAuctionItemId(lore)
        if (auctionId <= 0) return

        player.closeInventory()

        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val auctionInfo = plugin.auctionManager.getAuctionInfo(auctionId)

            plugin.server.scheduler.runTask(plugin, Runnable {
                if (auctionInfo == null) {
                    player.sendMessage("§c${plugin.langManager.getMessage(player, "auction.item_not_found")}")
                    return@Runnable
                }

                if (auctionInfo.sellerUuid == player.uniqueId) {
                    player.sendMessage("§c${plugin.langManager.getMessage(player, "ui.confirm_cancel")}")
                    player.sendMessage("§7${plugin.langManager.getMessage(player, "ui.type_confirm_cancel")}")
                    plugin.bidHandler.startCancellation(player, auctionId)
                } else {
                    player.sendMessage("§c${plugin.langManager.getMessage(player, "auction.not_your_auction")}")
                }
            })
        })
    }

    private fun handleMyBidsClick(player: Player, slot: Int, clickedItem: ItemStack, plugin: Main, isRightClick: Boolean) {
        when (slot) {
            in 0..35 -> handleMyBidsItemClick(player, clickedItem, plugin, isRightClick)
            48, 50 -> handlePaginationClick(player, clickedItem, plugin)
            49 -> return // Page info
            53 -> openMainUI(player, plugin) // Back
        }
    }

    private fun handleMyBidsItemClick(player: Player, clickedItem: ItemStack, plugin: Main, isRightClick: Boolean) {
        val meta = clickedItem.itemMeta ?: return
        val lore = meta.lore ?: return
        val auctionId = findAuctionItemId(lore)
        if (auctionId <= 0) return

        player.closeInventory()

        if (isRightClick) {
            player.sendMessage("§e${plugin.langManager.getMessage(player, "auction.cancelling_bid")}...")

            plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
                plugin.auctionManager.cancelPlayerBid(player, auctionId)

                plugin.server.scheduler.runTask(plugin, Runnable {
                    openMyBidsUI(player, plugin)
                })
            })
        } else {
            plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
                val auctionInfo = plugin.auctionManager.getAuctionInfo(auctionId, player.uniqueId)

                plugin.server.scheduler.runTask(plugin, Runnable {
                    if (auctionInfo != null) {
                        val currentPrice = auctionInfo.currentPrice
                        val playerBidAmount = auctionInfo.playerBidAmount ?: 0L
                        val timeRemaining = ItemUtils.formatTimeRemaining(auctionInfo.expiresAt, plugin.langManager, player)

                        player.sendMessage("§e━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                        player.sendMessage("§6${plugin.langManager.getMessage(player, "ui.change_bid_header")}")
                        player.sendMessage("§7${plugin.langManager.getMessage(player, "ui.current_highest_bid")}: §a${ItemUtils.formatPriceWithCurrency(currentPrice, plugin.getEconomyProvider(), plugin)}")
                        player.sendMessage("§7${plugin.langManager.getMessage(player, "ui.your_current_bid")}: §e${ItemUtils.formatPriceWithCurrency(playerBidAmount, plugin.getEconomyProvider(), plugin)}")
                        player.sendMessage("§7${plugin.langManager.getMessage(player, "ui.minimum_bid")}: §e${ItemUtils.formatPriceWithCurrency(currentPrice + 1, plugin.getEconomyProvider(), plugin)}")
                        if (auctionInfo.buyoutPrice != null) {
                            player.sendMessage("§7${plugin.langManager.getMessage(player, "ui.buyout_price")}: §6${ItemUtils.formatPriceWithCurrency(auctionInfo.buyoutPrice, plugin.getEconomyProvider(), plugin)}")
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
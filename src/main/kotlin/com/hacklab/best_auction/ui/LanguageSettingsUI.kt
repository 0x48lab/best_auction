package com.hacklab.best_auction.ui

import com.hacklab.best_auction.Main
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

class LanguageSettingsUI : Listener {
    
    companion object {
        private const val GUI_TITLE = "Language Settings"
        private const val GUI_SIZE = 27
        
        fun openLanguageSettings(player: Player, plugin: Main) {
            val inventory = Bukkit.createInventory(player, GUI_SIZE, plugin.langManager.getMessage(player, "ui.language_settings"))
            
            val currentLang = plugin.langManager.getPlayerLanguageSetting(player)
            
            // Auto-detect option
            val autoItem = createLanguageItem(
                Material.COMPASS,
                plugin.langManager.getMessage(player, "ui.language_auto"),
                plugin.langManager.getMessage(player, "ui.language_auto_desc"),
                currentLang == "auto"
            )
            inventory.setItem(10, autoItem)
            
            // English option
            val englishItem = createLanguageItem(
                Material.BOOK,
                "English",
                plugin.langManager.getMessage(player, "ui.language_english_desc"),
                currentLang == "en"
            )
            inventory.setItem(12, englishItem)
            
            // Japanese option
            val japaneseItem = createLanguageItem(
                Material.ENCHANTED_BOOK,
                "日本語 (Japanese)",
                plugin.langManager.getMessage(player, "ui.language_japanese_desc"),
                currentLang == "ja"
            )
            inventory.setItem(14, japaneseItem)
            
            // Back button
            val backItem = ItemStack(Material.ARROW)
            val backMeta = backItem.itemMeta
            backMeta?.setDisplayName("§a" + plugin.langManager.getMessage(player, "ui.back"))
            backItem.itemMeta = backMeta
            inventory.setItem(22, backItem)
            
            player.openInventory(inventory)
        }
        
        private fun createLanguageItem(material: Material, name: String, description: String, isSelected: Boolean): ItemStack {
            val item = ItemStack(material)
            val meta = item.itemMeta
            
            val displayName = if (isSelected) "§a§l$name §7(Selected)" else "§f$name"
            meta?.setDisplayName(displayName)
            
            val lore = mutableListOf<String>()
            lore.add("§7$description")
            lore.add("")
            if (isSelected) {
                lore.add("§a✓ Currently selected")
            } else {
                lore.add("§eClick to select")
            }
            meta?.lore = lore
            
            item.itemMeta = meta
            return item
        }
    }
    
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val inventory = event.inventory
        val plugin = Main.instance
        
        if (event.view.title != plugin.langManager.getMessage(player, "ui.language_settings")) {
            return
        }
        
        event.isCancelled = true
        
        when (event.slot) {
            10 -> { // Auto-detect
                if (plugin.langManager.setPlayerLanguage(player, "auto")) {
                    plugin.langManager.sendSuccessMessage(player, "ui.language_changed", "Auto-detect")
                    player.closeInventory()
                } else {
                    plugin.langManager.sendErrorMessage(player, "ui.language_change_failed")
                }
            }
            12 -> { // English
                if (plugin.langManager.setPlayerLanguage(player, "en")) {
                    plugin.langManager.sendSuccessMessage(player, "ui.language_changed", "English")
                    player.closeInventory()
                } else {
                    plugin.langManager.sendErrorMessage(player, "ui.language_change_failed")
                }
            }
            14 -> { // Japanese
                if (plugin.langManager.setPlayerLanguage(player, "ja")) {
                    plugin.langManager.sendSuccessMessage(player, "ui.language_changed", "日本語")
                    player.closeInventory()
                } else {
                    plugin.langManager.sendErrorMessage(player, "ui.language_change_failed")
                }
            }
            22 -> { // Back
                player.closeInventory()
                AuctionUI.openMainUI(player, plugin)
            }
        }
    }
}
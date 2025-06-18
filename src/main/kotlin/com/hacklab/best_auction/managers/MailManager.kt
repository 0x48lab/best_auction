package com.hacklab.best_auction.managers

import com.hacklab.best_auction.Main
import com.hacklab.best_auction.data.MailItem
import com.hacklab.best_auction.database.MailBox
import com.hacklab.best_auction.utils.ItemUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class MailManager(private val plugin: Main) {

    fun sendMail(playerUuid: UUID, playerName: String, item: ItemStack, reason: String) {
        transaction {
            MailBox.insert {
                it[MailBox.playerUuid] = playerUuid.toString()
                it[MailBox.playerName] = playerName
                it[itemData] = ItemUtils.serializeItemStack(item)
                it[MailBox.reason] = reason
            }
        }
    }

    fun getPlayerMail(player: Player): List<MailItem> {
        return transaction {
            MailBox.select { 
                (MailBox.playerUuid eq player.uniqueId.toString()) and 
                not(MailBox.isCollected) 
            }.map { row ->
                val item = ItemUtils.deserializeItemStack(row[MailBox.itemData])
                if (item != null) {
                    MailItem(
                        id = row[MailBox.id].value,
                        playerUuid = UUID.fromString(row[MailBox.playerUuid]),
                        playerName = row[MailBox.playerName],
                        itemStack = item,
                        reason = row[MailBox.reason],
                        createdAt = row[MailBox.createdAt]
                    )
                } else {
                    null
                }
            }.filterNotNull()
        }
    }

    fun collectMail(player: Player, mailId: Int): Boolean {
        return transaction {
            val mailItem = MailBox.select { 
                (MailBox.id eq mailId) and 
                (MailBox.playerUuid eq player.uniqueId.toString()) and 
                not(MailBox.isCollected) 
            }.singleOrNull()

            if (mailItem == null) {
                player.sendMessage("§cMail item not found!")
                return@transaction false
            }

            val item = ItemUtils.deserializeItemStack(mailItem[MailBox.itemData])
            if (item == null) {
                player.sendMessage("§cFailed to restore item!")
                return@transaction false
            }

            val leftover = player.inventory.addItem(item)
            if (leftover.isNotEmpty()) {
                player.sendMessage("§cInventory full! Clear some space and try again.")
                return@transaction false
            }

            MailBox.update({ MailBox.id eq mailId }) {
                it[isCollected] = true
            }

            player.sendMessage("§aItem collected!")
            true
        }
    }

    fun openMailBox(player: Player) {
        val mail = getPlayerMail(player)
        val title = plugin.langManager.getMessage(player, "ui.mailbox")
        val inventory = Bukkit.createInventory(null, 54, "§6$title")
        
        if (mail.isEmpty()) {
            // Empty mailbox - show info item
            val emptyItem = ItemStack(Material.PAPER)
            val emptyMeta = emptyItem.itemMeta!!
            emptyMeta.setDisplayName("§7${plugin.langManager.getMessage(player, "mail.no_mail")}")
            emptyMeta.lore = listOf("§7Your mailbox is currently empty.")
            emptyItem.itemMeta = emptyMeta
            inventory.setItem(22, emptyItem) // Center slot
        } else {
            // Display mail items
            mail.take(45).forEachIndexed { index, mailItem ->
                val displayItem = createMailDisplayItem(mailItem, player)
                inventory.setItem(index, displayItem)
            }
        }
        
        // Back button
        val backItem = ItemStack(Material.ARROW)
        val backMeta = backItem.itemMeta!!
        backMeta.setDisplayName("§c${plugin.langManager.getMessage(player, "ui.back")}")
        backItem.itemMeta = backMeta
        inventory.setItem(53, backItem)
        
        player.openInventory(inventory)
    }
    
    private fun createMailDisplayItem(mailItem: MailItem, player: Player): ItemStack {
        val displayItem = mailItem.itemStack.clone()
        val originalMeta = mailItem.itemStack.itemMeta
        val meta = displayItem.itemMeta!!
        
        // アイテムの正式名前
        val officialName = displayItem.type.name.replace("_", " ").lowercase()
            .split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
        
        // 表示名
        val displayName = if (originalMeta?.hasDisplayName() == true) {
            originalMeta.displayName!!
        } else {
            officialName
        }
        
        meta.setDisplayName("§6$displayName")
        
        val lore = mutableListOf<String>()
        lore.add("§7アイテム正式名: §f$officialName")
        if (originalMeta?.hasDisplayName() == true) {
            lore.add("§7設定名: §f${originalMeta.displayName}")
        }
        val playerLang = plugin.langManager.getPlayerLanguageSetting(player)
        val reasonMsg = plugin.langManager.getMessage(player, "mail.mail_reason", "§f${mailItem.reason}")
        val dateMsg = plugin.langManager.getMessage(player, "mail.mail_date", "§f${com.hacklab.best_auction.utils.ItemUtils.formatDate(mailItem.createdAt, plugin)}")
        plugin.logger.info("MailBox Debug - Player: ${player.name}, Lang: $playerLang, Reason msg: '$reasonMsg', Date msg: '$dateMsg'")
        
        lore.add("§7$reasonMsg")
        lore.add("§7$dateMsg")
        lore.add("")
        lore.add("§e${plugin.langManager.getMessage(player, "mail.mail_collect")}")
        lore.add("")
        lore.add("§8ID: ${mailItem.id}")
        
        meta.lore = lore
        displayItem.itemMeta = meta
        
        return displayItem
    }
    
    fun handleMailBoxClick(player: Player, clickedItem: ItemStack): Boolean {
        if (clickedItem.type == Material.ARROW) {
            // Back to main auction GUI
            return true // Let caller handle this
        }
        
        if (clickedItem.type == Material.PAPER) {
            // Empty mailbox indicator - do nothing
            return false
        }
        
        val meta = clickedItem.itemMeta ?: return false
        val lore = meta.lore ?: return false
        
        // Extract mail ID from lore
        val idLine = lore.find { it.startsWith("§8ID: ") }
        val mailId = idLine?.replace("§8ID: ", "")?.toIntOrNull() ?: return false
        
        // Collect the mail
        if (collectMail(player, mailId)) {
            // Refresh mailbox after successful collection
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                openMailBox(player)
            }, 1L)
        }
        
        return false
    }
}
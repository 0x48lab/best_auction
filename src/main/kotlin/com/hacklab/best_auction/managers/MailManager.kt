package com.hacklab.best_auction.managers

import com.hacklab.best_auction.data.MailItem
import com.hacklab.best_auction.database.MailBox
import com.hacklab.best_auction.utils.ItemUtils
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class MailManager {

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
        if (mail.isEmpty()) {
            player.sendMessage("§eYour mailbox is empty!")
            return
        }

        player.sendMessage("§6=== Your Mailbox ===")
        mail.forEachIndexed { index, mailItem ->
            val itemName = mailItem.itemStack.itemMeta?.displayName ?: mailItem.itemStack.type.name
            player.sendMessage("§e${index + 1}. §f$itemName §7(${mailItem.reason})")
        }
        player.sendMessage("§7Use /auction mail collect <number> to collect items")
    }
}
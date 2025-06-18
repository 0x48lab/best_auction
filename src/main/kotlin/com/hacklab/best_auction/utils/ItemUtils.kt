package com.hacklab.best_auction.utils

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*

object ItemUtils {
    
    fun serializeItemStack(item: ItemStack): String {
        return try {
            val outputStream = ByteArrayOutputStream()
            val dataOutput = BukkitObjectOutputStream(outputStream)
            dataOutput.writeObject(item)
            dataOutput.close()
            Base64.getEncoder().encodeToString(outputStream.toByteArray())
        } catch (e: Exception) {
            ""
        }
    }
    
    fun deserializeItemStack(data: String): ItemStack? {
        return try {
            val inputStream = ByteArrayInputStream(Base64.getDecoder().decode(data))
            val dataInput = BukkitObjectInputStream(inputStream)
            val item = dataInput.readObject() as ItemStack
            dataInput.close()
            item
        } catch (e: Exception) {
            null
        }
    }
    
    fun calculateFee(price: Long, isStack: Boolean): Long {
        val baseFee = if (isStack) (price * 0.005).toLong() + 4 else (price * 0.01).toLong() + 1
        return minOf(baseFee, 10000L)
    }
    
    fun removeItemName(item: ItemStack): ItemStack {
        val clone = item.clone()
        val meta = clone.itemMeta
        if (meta != null && meta.hasDisplayName()) {
            meta.setDisplayName(null)
            clone.itemMeta = meta
        }
        return clone
    }
    
    fun isValidForAuction(item: ItemStack): Pair<Boolean, String> {
        if (item.type.isAir) {
            return false to "Cannot auction air!"
        }
        
        if (item.amount != 1 && item.amount != item.maxStackSize) {
            return false to "Items must be sold individually or in full stacks only!"
        }
        
        val meta = item.itemMeta
        if (meta != null) {
            if (meta.hasEnchants() && item.durability > 0) {
                return false to "Enchanted items must be at full durability!"
            }
        }
        
        return true to ""
    }
    
    fun formatPrice(price: Long): String {
        return when {
            price >= 1000000 -> String.format("%.2fM", price / 1000000.0)
            price >= 1000 -> String.format("%.1fK", price / 1000.0)
            else -> price.toString()
        }
    }
}
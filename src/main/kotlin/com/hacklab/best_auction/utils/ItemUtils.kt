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
    
    fun isValidForAuction(item: ItemStack, langManager: com.hacklab.best_auction.utils.LangManager? = null, player: org.bukkit.entity.Player? = null): Pair<Boolean, String> {
        if (item.type.isAir) {
            val message = if (langManager != null && player != null) {
                langManager.getMessage(player, "auction.cannot_auction_air")
            } else {
                "Cannot auction air!"
            }
            return false to message
        }
        
        // Note: Stack quantity validation is now handled in AuctionManager based on config
        
        val meta = item.itemMeta
        if (meta != null) {
            if (meta.hasEnchants() && item.durability > 0) {
                val message = if (langManager != null && player != null) {
                    langManager.getMessage(player, "auction.enchanted_items_full_durability")
                } else {
                    "Enchanted items must be at full durability!"
                }
                return false to message
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
    
    fun formatPriceWithCurrency(price: Long, economy: net.milkbowl.vault.economy.Economy?, plugin: com.hacklab.best_auction.Main? = null): String {
        return try {
            val formattedAmount = when {
                price >= 1000000 -> String.format("%.2fM", price / 1000000.0)
                price >= 1000 -> String.format("%.1fK", price / 1000.0)
                else -> price.toString()
            }
            
            // Get fallback currency from config
            val fallbackCurrency = plugin?.config?.getString("currency.fallback_currency") ?: "gil"
            val useVaultFormat = plugin?.config?.getBoolean("currency.use_vault_format") ?: true
            val currencySymbol = plugin?.config?.getString("currency.currency_symbol") ?: ""
            
            // If Vault is disabled in config or economy is null, use manual format
            if (!useVaultFormat || economy == null) {
                return if (currencySymbol.isNotBlank()) {
                    "$currencySymbol$formattedAmount"
                } else {
                    "$formattedAmount $fallbackCurrency"
                }
            }
            
            // Try to get currency format from Vault
            val vaultFormatted = economy.format(price.toDouble())
            
            // If Vault formatting fails or returns unexpected result, use fallback
            if (vaultFormatted.isNullOrBlank() || !vaultFormatted.contains(price.toString())) {
                val currencyName = economy.currencyNamePlural()?.takeIf { it.isNotBlank() } ?: fallbackCurrency
                return "$formattedAmount $currencyName"
            }
            
            // Replace full amount with abbreviated amount but keep currency format
            vaultFormatted.replace(price.toString(), formattedAmount)
        } catch (e: Exception) {
            // Ultimate fallback to default format if any error occurs
            val fallbackCurrency = plugin?.config?.getString("currency.fallback_currency") ?: "gil"
            "${formatPrice(price)} $fallbackCurrency"
        }
    }
    
    fun parseAmount(input: String): Long? {
        val cleanInput = input.replace(",", "").replace(" ", "").lowercase()
        
        return when {
            cleanInput.endsWith("k") -> {
                val number = cleanInput.dropLast(1).toDoubleOrNull()
                if (number != null && number >= 0) (number * 1000).toLong() else null
            }
            cleanInput.endsWith("m") -> {
                val number = cleanInput.dropLast(1).toDoubleOrNull()
                if (number != null && number >= 0) (number * 1000000).toLong() else null
            }
            cleanInput.endsWith("b") -> {
                val number = cleanInput.dropLast(1).toDoubleOrNull()
                if (number != null && number >= 0) (number * 1000000000).toLong() else null
            }
            else -> {
                val number = cleanInput.toLongOrNull()
                if (number != null && number >= 0) number else null
            }
        }
    }
    
    fun formatTimeRemaining(expiresAt: java.time.LocalDateTime, langManager: com.hacklab.best_auction.utils.LangManager, player: org.bukkit.entity.Player): String {
        val now = java.time.LocalDateTime.now()
        val duration = java.time.Duration.between(now, expiresAt)
        
        return when {
            duration.isNegative || duration.isZero -> langManager.getMessage(player, "time.expired")
            duration.toDays() > 0 -> {
                val days = duration.toDays()
                val hours = duration.toHours() % 24
                if (hours > 0) {
                    langManager.getMessage(player, "time.days_hours", "$days", "$hours")
                } else {
                    langManager.getMessage(player, "time.days", "$days")
                }
            }
            duration.toHours() > 0 -> {
                val hours = duration.toHours()
                val minutes = (duration.toMinutes() % 60)
                if (minutes > 0) {
                    langManager.getMessage(player, "time.hours_minutes", "$hours", "$minutes")
                } else {
                    langManager.getMessage(player, "time.hours", "$hours")
                }
            }
            duration.toMinutes() > 0 -> {
                langManager.getMessage(player, "time.minutes", "${duration.toMinutes()}")
            }
            else -> {
                langManager.getMessage(player, "time.seconds", "${duration.seconds}")
            }
        }
    }
    
    fun formatDate(dateTime: java.time.LocalDateTime, plugin: com.hacklab.best_auction.Main?): String {
        return try {
            val dateFormat = plugin?.config?.getString("ui.date_format") ?: "yyyy-MM-dd"
            val formatter = java.time.format.DateTimeFormatter.ofPattern(dateFormat)
            dateTime.format(formatter)
        } catch (e: Exception) {
            // Fallback to default format if pattern is invalid
            dateTime.toLocalDate().toString()
        }
    }
}
package com.hacklab.best_auction.data

import org.bukkit.inventory.ItemStack
import java.time.LocalDateTime
import java.util.*

data class AuctionItem(
    val id: Int,
    val sellerUuid: UUID,
    val sellerName: String,
    val itemStack: ItemStack,
    val startPrice: Long,
    val buyoutPrice: Long?,
    val currentPrice: Long,
    val category: String,
    val listingFee: Long,
    val createdAt: LocalDateTime,
    val expiresAt: LocalDateTime,
    val isActive: Boolean = true,
    val isSold: Boolean = false,
    val quantity: Int = 1,
    val playerBidAmount: Long? = null // For tracking player's bid amount when viewing their bids
)

data class Bid(
    val id: Int,
    val auctionItemId: Int,
    val bidderUuid: UUID,
    val bidderName: String,
    val bidAmount: Long,
    val createdAt: LocalDateTime
)

data class MailItem(
    val id: Int,
    val playerUuid: UUID,
    val playerName: String,
    val itemStack: ItemStack,
    val reason: String,
    val createdAt: LocalDateTime,
    val isCollected: Boolean = false
)

data class PlayerSetting(
    val id: Int,
    val playerUuid: UUID,
    val setting: String,
    val value: String,
    val updatedAt: LocalDateTime
)

enum class AuctionCategory(val displayName: String, val material: org.bukkit.Material) {
    ALL("All", org.bukkit.Material.CHEST),
    BLOCKS("Blocks", org.bukkit.Material.STONE),
    FOOD("Food", org.bukkit.Material.APPLE),
    WEAPONS_EQUIPMENT("Weapons & Equipment", org.bukkit.Material.DIAMOND_SWORD),
    ENCHANTMENTS("Enchantments", org.bukkit.Material.ENCHANTED_BOOK),
    OTHERS("Others", org.bukkit.Material.LAVA_BUCKET);

    companion object {
        // Legacy category name mapping for backward compatibility with existing DB data
        private val legacyCategoryMap = mapOf(
            "BUILDING_BLOCKS" to BLOCKS,
            "DECORATIONS" to BLOCKS,
            "FOOD" to FOOD,
            "COMBAT" to WEAPONS_EQUIPMENT,
            "TOOLS" to WEAPONS_EQUIPMENT,
            "ENCHANTED_BOOKS" to ENCHANTMENTS,
            "REDSTONE" to OTHERS,
            "TRANSPORTATION" to OTHERS,
            "BREWING" to OTHERS,
            "MISCELLANEOUS" to OTHERS
        )

        // DB category names that map to each new category (for query filtering)
        val categoryDbNames = mapOf(
            BLOCKS to listOf("BLOCKS", "BUILDING_BLOCKS", "DECORATIONS"),
            FOOD to listOf("FOOD"),
            WEAPONS_EQUIPMENT to listOf("WEAPONS_EQUIPMENT", "COMBAT", "TOOLS"),
            ENCHANTMENTS to listOf("ENCHANTMENTS", "ENCHANTED_BOOKS"),
            OTHERS to listOf("OTHERS", "REDSTONE", "TRANSPORTATION", "BREWING", "MISCELLANEOUS")
        )

        fun fromLegacyName(name: String): AuctionCategory {
            // First try direct match with new category names
            return try {
                valueOf(name)
            } catch (e: IllegalArgumentException) {
                // Fall back to legacy mapping
                legacyCategoryMap[name] ?: OTHERS
            }
        }

        fun fromItemStack(itemStack: ItemStack, plugin: com.hacklab.best_auction.Main): AuctionCategory {
            val material = itemStack.type
            val materialName = material.name

            // 1. Highest priority: Enchantment check (enchanted items go to ENCHANTMENTS)
            if (material == org.bukkit.Material.ENCHANTED_BOOK) {
                return ENCHANTMENTS
            }
            if (itemStack.itemMeta?.hasEnchants() == true) {
                return ENCHANTMENTS
            }

            // 2. Food
            if (material.isEdible) {
                return FOOD
            }

            // 3. Weapons & Equipment (combat items + tools)
            if (materialName.endsWith("_SWORD") || materialName.endsWith("_AXE") || materialName.endsWith("_HELMET") ||
                materialName.endsWith("_CHESTPLATE") || materialName.endsWith("_LEGGINGS") || materialName.endsWith("_BOOTS") ||
                material in setOf(org.bukkit.Material.BOW, org.bukkit.Material.CROSSBOW, org.bukkit.Material.TRIDENT, org.bukkit.Material.SHIELD, org.bukkit.Material.ARROW, org.bukkit.Material.SPECTRAL_ARROW, org.bukkit.Material.TIPPED_ARROW)) {
                return WEAPONS_EQUIPMENT
            }
            if (materialName.endsWith("_PICKAXE") || materialName.endsWith("_SHOVEL") || materialName.endsWith("_HOE") ||
                material in setOf(org.bukkit.Material.FISHING_ROD, org.bukkit.Material.FLINT_AND_STEEL, org.bukkit.Material.SHEARS, org.bukkit.Material.COMPASS, org.bukkit.Material.CLOCK, org.bukkit.Material.LEAD, org.bukkit.Material.NAME_TAG)) {
                return WEAPONS_EQUIPMENT
            }

            // 4. Blocks (building blocks + decorations)
            if (materialName.endsWith("_DYE") || materialName.endsWith("_BANNER") || materialName.endsWith("_CARPET") ||
                materialName.endsWith("_BED") || materialName.contains("_HEAD") || materialName.contains("_SKULL") ||
                materialName.contains("FLOWER") || materialName.endsWith("_SAPLING") || materialName.endsWith("_LEAVES") ||
                material in setOf(org.bukkit.Material.CRAFTING_TABLE, org.bukkit.Material.FURNACE, org.bukkit.Material.ANVIL, org.bukkit.Material.ENCHANTING_TABLE, org.bukkit.Material.SMOKER, org.bukkit.Material.BLAST_FURNACE, org.bukkit.Material.CARTOGRAPHY_TABLE, org.bukkit.Material.FLETCHING_TABLE, org.bukkit.Material.GRINDSTONE, org.bukkit.Material.LOOM, org.bukkit.Material.SMITHING_TABLE, org.bukkit.Material.STONECUTTER, org.bukkit.Material.ITEM_FRAME, org.bukkit.Material.PAINTING, org.bukkit.Material.ARMOR_STAND, org.bukkit.Material.FLOWER_POT, org.bukkit.Material.CANDLE, org.bukkit.Material.LANTERN, org.bukkit.Material.SOUL_LANTERN)) {
                return BLOCKS
            }
            if (material.isBlock) {
                return BLOCKS
            }

            // 5. Fallback: Others
            return OTHERS
        }
    }
}
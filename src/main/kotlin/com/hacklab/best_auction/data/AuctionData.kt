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
    val quantity: Int = 1
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
    BUILDING_BLOCKS("Building Blocks", org.bukkit.Material.STONE),
    DECORATIONS("Decorations", org.bukkit.Material.PEONY),
    REDSTONE("Redstone", org.bukkit.Material.REDSTONE),
    TRANSPORTATION("Transportation", org.bukkit.Material.MINECART),
    MISCELLANEOUS("Miscellaneous", org.bukkit.Material.LAVA_BUCKET),
    FOOD("Food & Drinks", org.bukkit.Material.APPLE),
    TOOLS("Tools", org.bukkit.Material.DIAMOND_PICKAXE),
    COMBAT("Combat", org.bukkit.Material.DIAMOND_SWORD),
    BREWING("Brewing", org.bukkit.Material.BREWING_STAND);

    companion object {
        fun fromMaterial(material: org.bukkit.Material): AuctionCategory {
            val materialName = material.name
            
            return when {
                // Building blocks
                materialName in listOf("STONE", "COBBLESTONE", "GRANITE", "DIORITE", "ANDESITE", "OAK_PLANKS", "SPRUCE_PLANKS", "BIRCH_PLANKS", "JUNGLE_PLANKS", "ACACIA_PLANKS", "DARK_OAK_PLANKS", "DIRT", "GRASS_BLOCK", "COARSE_DIRT", "PODZOL", "SAND", "RED_SAND", "GRAVEL", "CLAY", "TERRACOTTA", "WHITE_TERRACOTTA", "ORANGE_TERRACOTTA", "MAGENTA_TERRACOTTA", "LIGHT_BLUE_TERRACOTTA", "YELLOW_TERRACOTTA", "LIME_TERRACOTTA", "PINK_TERRACOTTA", "GRAY_TERRACOTTA", "LIGHT_GRAY_TERRACOTTA", "CYAN_TERRACOTTA", "PURPLE_TERRACOTTA", "BLUE_TERRACOTTA", "BROWN_TERRACOTTA", "GREEN_TERRACOTTA", "RED_TERRACOTTA", "BLACK_TERRACOTTA") -> BUILDING_BLOCKS
                materialName.contains("_PLANKS") || materialName.contains("_LOG") || materialName.contains("_WOOD") || materialName.contains("STONE") -> BUILDING_BLOCKS
                
                // Combat - weapons and armor
                materialName.contains("SWORD") || materialName.contains("BOW") || materialName in listOf("CROSSBOW", "SHIELD", "TRIDENT") -> COMBAT
                materialName.contains("_HELMET") || materialName.contains("_CHESTPLATE") || materialName.contains("_LEGGINGS") || materialName.contains("_BOOTS") -> COMBAT
                
                // Tools
                materialName.contains("PICKAXE") || materialName.contains("AXE") || materialName.contains("SHOVEL") || materialName.contains("HOE") -> TOOLS
                materialName in listOf("FISHING_ROD", "FLINT_AND_STEEL", "SHEARS", "LEAD", "NAME_TAG") -> TOOLS
                
                // Food
                materialName in listOf("APPLE", "BREAD", "COOKED_BEEF", "COOKED_CHICKEN", "COOKED_PORKCHOP", "COOKED_MUTTON", "COOKED_RABBIT", "COOKED_COD", "COOKED_SALMON", "GOLDEN_APPLE", "ENCHANTED_GOLDEN_APPLE", "GOLDEN_CARROT", "BAKED_POTATO", "COOKIE", "PUMPKIN_PIE", "CAKE", "BEETROOT_SOUP", "MUSHROOM_STEW", "RABBIT_STEW", "SUSPICIOUS_STEW") -> FOOD
                materialName.contains("COOKED_") || materialName.contains("STEW") || materialName.contains("SOUP") -> FOOD
                
                // Redstone
                materialName in listOf("REDSTONE", "REPEATER", "COMPARATOR", "PISTON", "STICKY_PISTON", "TNT", "HOPPER", "DROPPER", "DISPENSER", "OBSERVER", "DAYLIGHT_DETECTOR", "TRIPWIRE_HOOK", "LEVER", "REDSTONE_TORCH", "REDSTONE_LAMP") -> REDSTONE
                materialName.contains("REDSTONE") || materialName.contains("_BUTTON") || materialName.contains("PRESSURE_PLATE") -> REDSTONE
                
                // Transportation
                materialName in listOf("MINECART", "SADDLE", "HORSE_ARMOR", "ELYTRA", "CARROT_ON_A_STICK", "WARPED_FUNGUS_ON_A_STICK") -> TRANSPORTATION
                materialName.contains("BOAT") || materialName.contains("MINECART") || materialName.contains("RAILS") -> TRANSPORTATION
                
                // Brewing
                materialName in listOf("BREWING_STAND", "CAULDRON", "POTION", "SPLASH_POTION", "LINGERING_POTION", "DRAGON_BREATH", "FERMENTED_SPIDER_EYE", "BLAZE_POWDER", "MAGMA_CREAM", "GHAST_TEAR", "SPIDER_EYE", "SUGAR", "GLISTERING_MELON_SLICE", "RABBIT_FOOT", "PUFFERFISH") -> BREWING
                materialName.contains("POTION") -> BREWING
                
                // Decorations
                materialName in listOf("FLOWER_POT", "ITEM_FRAME", "PAINTING", "ARMOR_STAND", "BANNER", "CARPET", "GLAZED_TERRACOTTA") -> DECORATIONS
                materialName.contains("_CARPET") || materialName.contains("_BANNER") || materialName.contains("FLOWER") || materialName.contains("_GLAZED_TERRACOTTA") -> DECORATIONS
                
                else -> MISCELLANEOUS
            }
        }
    }
}
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
    BUILDING_BLOCKS("Building Blocks", org.bukkit.Material.STONE),
    DECORATIONS("Decorations", org.bukkit.Material.PEONY),
    REDSTONE("Redstone", org.bukkit.Material.REDSTONE),
    TRANSPORTATION("Transportation", org.bukkit.Material.MINECART),
    MISCELLANEOUS("Miscellaneous", org.bukkit.Material.LAVA_BUCKET),
    FOOD("Food & Drinks", org.bukkit.Material.APPLE),
    TOOLS("Tools", org.bukkit.Material.DIAMOND_PICKAXE),
    COMBAT("Combat", org.bukkit.Material.DIAMOND_SWORD),
    BREWING("Brewing", org.bukkit.Material.BREWING_STAND),
    ENCHANTED_BOOKS("Enchanted Books", org.bukkit.Material.ENCHANTED_BOOK);

    companion object {
        fun fromItemStack(itemStack: ItemStack, plugin: com.hacklab.best_auction.Main): AuctionCategory {
            val material = itemStack.type
            val materialName = material.name
            plugin.logger.info("[CategoryDebug] Checking item: $materialName, Meta: ${itemStack.itemMeta?.javaClass?.simpleName ?: "None"}")

            // Using a clear if-else-if structure to ensure correct priority.

            // 1. Highest priority checks for specific types
            if (material == org.bukkit.Material.ENCHANTED_BOOK) {
                plugin.logger.info("[CategoryDebug] Matched ENCHANTED_BOOK by material.")
                return ENCHANTED_BOOKS
            } else if (itemStack.itemMeta is org.bukkit.inventory.meta.PotionMeta) {
                plugin.logger.info("[CategoryDebug] Matched Potion by meta.")
                return BREWING
            }

            // 2. Combat Items
            else if (materialName.endsWith("_SWORD") || materialName.endsWith("_AXE") || materialName.endsWith("_HELMET") ||
                materialName.endsWith("_CHESTPLATE") || materialName.endsWith("_LEGGINGS") || materialName.endsWith("_BOOTS") ||
                material in setOf(org.bukkit.Material.BOW, org.bukkit.Material.CROSSBOW, org.bukkit.Material.TRIDENT, org.bukkit.Material.SHIELD, org.bukkit.Material.ARROW, org.bukkit.Material.SPECTRAL_ARROW, org.bukkit.Material.TIPPED_ARROW)) {
                return COMBAT
            }

            // 3. Tools
            else if (materialName.endsWith("_PICKAXE") || materialName.endsWith("_SHOVEL") || materialName.endsWith("_HOE") ||
                material in setOf(org.bukkit.Material.FISHING_ROD, org.bukkit.Material.FLINT_AND_STEEL, org.bukkit.Material.SHEARS, org.bukkit.Material.COMPASS, org.bukkit.Material.CLOCK, org.bukkit.Material.LEAD, org.bukkit.Material.NAME_TAG)) {
                return TOOLS
            }

            // 4. Redstone
            else if (materialName.contains("REDSTONE") || materialName.endsWith("_BUTTON") || materialName.endsWith("_PLATE") ||
                materialName.endsWith("DOOR") || materialName.endsWith("GATE") || materialName.endsWith("TRAPDOOR") ||
                material in setOf(org.bukkit.Material.PISTON, org.bukkit.Material.STICKY_PISTON, org.bukkit.Material.OBSERVER, org.bukkit.Material.DISPENSER, org.bukkit.Material.DROPPER, org.bukkit.Material.HOPPER, org.bukkit.Material.COMPARATOR, org.bukkit.Material.REPEATER, org.bukkit.Material.LEVER, org.bukkit.Material.TRIPWIRE_HOOK, org.bukkit.Material.DAYLIGHT_DETECTOR, org.bukkit.Material.TNT, org.bukkit.Material.REDSTONE_LAMP)) {
                return REDSTONE
            }

            // 5. Transportation
            else if (materialName.endsWith("_BOAT") || materialName.endsWith("_MINECART") || materialName.contains("RAIL") ||
                material in setOf(org.bukkit.Material.SADDLE, org.bukkit.Material.ELYTRA, org.bukkit.Material.CARROT_ON_A_STICK, org.bukkit.Material.WARPED_FUNGUS_ON_A_STICK)) {
                return TRANSPORTATION
            }

            // 6. Brewing Ingredients (excluding potions, which are handled by meta)
            else if (material in setOf(org.bukkit.Material.BREWING_STAND, org.bukkit.Material.CAULDRON, org.bukkit.Material.DRAGON_BREATH, org.bukkit.Material.FERMENTED_SPIDER_EYE, org.bukkit.Material.BLAZE_POWDER, org.bukkit.Material.MAGMA_CREAM, org.bukkit.Material.GHAST_TEAR, org.bukkit.Material.SPIDER_EYE, org.bukkit.Material.SUGAR, org.bukkit.Material.GLISTERING_MELON_SLICE, org.bukkit.Material.RABBIT_FOOT, org.bukkit.Material.PUFFERFISH)) {
                return BREWING
            }

            // 7. Decorations and Functional Blocks
            else if (materialName.endsWith("_DYE") || materialName.endsWith("_BANNER") || materialName.endsWith("_CARPET") ||
                materialName.endsWith("_BED") || materialName.contains("_HEAD") || materialName.contains("_SKULL") ||
                materialName.contains("FLOWER") || materialName.endsWith("_SAPLING") || materialName.endsWith("_LEAVES") ||
                material in setOf(org.bukkit.Material.CRAFTING_TABLE, org.bukkit.Material.FURNACE, org.bukkit.Material.ANVIL, org.bukkit.Material.ENCHANTING_TABLE, org.bukkit.Material.SMOKER, org.bukkit.Material.BLAST_FURNACE, org.bukkit.Material.CARTOGRAPHY_TABLE, org.bukkit.Material.FLETCHING_TABLE, org.bukkit.Material.GRINDSTONE, org.bukkit.Material.LOOM, org.bukkit.Material.SMITHING_TABLE, org.bukkit.Material.STONECUTTER, org.bukkit.Material.ITEM_FRAME, org.bukkit.Material.PAINTING, org.bukkit.Material.ARMOR_STAND, org.bukkit.Material.FLOWER_POT, org.bukkit.Material.CANDLE, org.bukkit.Material.LANTERN, org.bukkit.Material.SOUL_LANTERN)) {
                plugin.logger.info("[CategoryDebug] Matched DECORATIONS.")
                return DECORATIONS
            }

            // 8. General Properties (lower priority)
            else if (material.isEdible) {
                plugin.logger.info("[CategoryDebug] Matched FOOD by property.")
                return FOOD
            }
            else if (material.isBlock) {
                plugin.logger.info("[CategoryDebug] Matched BUILDING_BLOCKS by property.")
                return BUILDING_BLOCKS
            }

            // 9. Fallback
            else {
                plugin.logger.info("[CategoryDebug] No match found, falling back to MISCELLANEOUS.")
                return MISCELLANEOUS
            }
        }
    }
}
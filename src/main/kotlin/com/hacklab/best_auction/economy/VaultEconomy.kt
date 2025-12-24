package com.hacklab.best_auction.economy

import net.milkbowl.vault.economy.Economy
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

/**
 * Vault経済システムのラッパー
 * 既存のVault/VaultUnlocked経済プラグインと連携する
 */
class VaultEconomy(private val vaultEconomy: Economy) : EconomyProvider {

    override val name: String = vaultEconomy.name

    override fun currencyNamePlural(): String {
        return vaultEconomy.currencyNamePlural() ?: "coins"
    }

    override fun format(amount: Double): String {
        return vaultEconomy.format(amount)
    }

    override fun has(player: Player, amount: Double): Boolean {
        return vaultEconomy.has(player, amount)
    }

    override fun has(player: OfflinePlayer, amount: Double): Boolean {
        return vaultEconomy.has(player, amount)
    }

    override fun hasAccount(player: Player): Boolean {
        return vaultEconomy.hasAccount(player)
    }

    override fun hasAccount(player: OfflinePlayer): Boolean {
        return vaultEconomy.hasAccount(player)
    }

    override fun withdrawPlayer(player: Player, amount: Double): Boolean {
        return vaultEconomy.withdrawPlayer(player, amount).transactionSuccess()
    }

    override fun withdrawPlayer(player: OfflinePlayer, amount: Double): Boolean {
        return vaultEconomy.withdrawPlayer(player, amount).transactionSuccess()
    }

    override fun depositPlayer(player: Player, amount: Double): Boolean {
        return vaultEconomy.depositPlayer(player, amount).transactionSuccess()
    }

    override fun depositPlayer(player: OfflinePlayer, amount: Double): Boolean {
        return vaultEconomy.depositPlayer(player, amount).transactionSuccess()
    }

    override fun getBalance(player: Player): Double {
        return vaultEconomy.getBalance(player)
    }

    override fun getBalance(player: OfflinePlayer): Double {
        return vaultEconomy.getBalance(player)
    }
}

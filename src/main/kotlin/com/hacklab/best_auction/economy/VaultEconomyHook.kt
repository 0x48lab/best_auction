package com.hacklab.best_auction.economy

import com.hacklab.best_auction.Main
import net.milkbowl.vault.economy.Economy
import net.milkbowl.vault.economy.EconomyResponse
import org.bukkit.OfflinePlayer

/**
 * 内蔵経済システムをVault APIとして公開するためのフック
 * これにより他のプラグインがVault経由で内蔵経済を使用できる
 */
class VaultEconomyHook(private val plugin: Main, private val internalEconomy: InternalEconomy) : Economy {

    override fun isEnabled(): Boolean = true

    override fun getName(): String = "BestAuction"

    override fun hasBankSupport(): Boolean = false

    override fun fractionalDigits(): Int = 0

    override fun format(amount: Double): String = internalEconomy.format(amount)

    override fun currencyNamePlural(): String = internalEconomy.currencyNamePlural()

    override fun currencyNameSingular(): String = internalEconomy.currencyNamePlural()

    override fun hasAccount(playerName: String): Boolean {
        val player = plugin.server.getOfflinePlayer(playerName)
        return internalEconomy.hasAccount(player)
    }

    override fun hasAccount(player: OfflinePlayer): Boolean = internalEconomy.hasAccount(player)

    override fun hasAccount(playerName: String, worldName: String): Boolean = hasAccount(playerName)

    override fun hasAccount(player: OfflinePlayer, worldName: String): Boolean = hasAccount(player)

    override fun getBalance(playerName: String): Double {
        val player = plugin.server.getOfflinePlayer(playerName)
        return internalEconomy.getBalance(player)
    }

    override fun getBalance(player: OfflinePlayer): Double = internalEconomy.getBalance(player)

    override fun getBalance(playerName: String, world: String): Double = getBalance(playerName)

    override fun getBalance(player: OfflinePlayer, world: String): Double = getBalance(player)

    override fun has(playerName: String, amount: Double): Boolean {
        val player = plugin.server.getOfflinePlayer(playerName)
        return internalEconomy.has(player, amount)
    }

    override fun has(player: OfflinePlayer, amount: Double): Boolean = internalEconomy.has(player, amount)

    override fun has(playerName: String, worldName: String, amount: Double): Boolean = has(playerName, amount)

    override fun has(player: OfflinePlayer, worldName: String, amount: Double): Boolean = has(player, amount)

    override fun withdrawPlayer(playerName: String, amount: Double): EconomyResponse {
        val player = plugin.server.getOfflinePlayer(playerName)
        return withdrawPlayer(player, amount)
    }

    override fun withdrawPlayer(player: OfflinePlayer, amount: Double): EconomyResponse {
        return if (internalEconomy.withdrawPlayer(player, amount)) {
            EconomyResponse(amount, getBalance(player), EconomyResponse.ResponseType.SUCCESS, null)
        } else {
            EconomyResponse(0.0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Insufficient funds")
        }
    }

    override fun withdrawPlayer(playerName: String, worldName: String, amount: Double): EconomyResponse = withdrawPlayer(playerName, amount)

    override fun withdrawPlayer(player: OfflinePlayer, worldName: String, amount: Double): EconomyResponse = withdrawPlayer(player, amount)

    override fun depositPlayer(playerName: String, amount: Double): EconomyResponse {
        val player = plugin.server.getOfflinePlayer(playerName)
        return depositPlayer(player, amount)
    }

    override fun depositPlayer(player: OfflinePlayer, amount: Double): EconomyResponse {
        return if (internalEconomy.depositPlayer(player, amount)) {
            EconomyResponse(amount, getBalance(player), EconomyResponse.ResponseType.SUCCESS, null)
        } else {
            EconomyResponse(0.0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Deposit failed")
        }
    }

    override fun depositPlayer(playerName: String, worldName: String, amount: Double): EconomyResponse = depositPlayer(playerName, amount)

    override fun depositPlayer(player: OfflinePlayer, worldName: String, amount: Double): EconomyResponse = depositPlayer(player, amount)

    override fun createPlayerAccount(playerName: String): Boolean {
        val player = plugin.server.getOfflinePlayer(playerName)
        return createPlayerAccount(player)
    }

    override fun createPlayerAccount(player: OfflinePlayer): Boolean {
        // InternalEconomyはensureAccountで自動作成するので、残高取得で作成される
        internalEconomy.getBalance(player)
        return true
    }

    override fun createPlayerAccount(playerName: String, worldName: String): Boolean = createPlayerAccount(playerName)

    override fun createPlayerAccount(player: OfflinePlayer, worldName: String): Boolean = createPlayerAccount(player)

    // Bank methods - not supported
    override fun createBank(name: String, player: String): EconomyResponse = notSupported()
    override fun createBank(name: String, player: OfflinePlayer): EconomyResponse = notSupported()
    override fun deleteBank(name: String): EconomyResponse = notSupported()
    override fun bankBalance(name: String): EconomyResponse = notSupported()
    override fun bankHas(name: String, amount: Double): EconomyResponse = notSupported()
    override fun bankWithdraw(name: String, amount: Double): EconomyResponse = notSupported()
    override fun bankDeposit(name: String, amount: Double): EconomyResponse = notSupported()
    override fun isBankOwner(name: String, playerName: String): EconomyResponse = notSupported()
    override fun isBankOwner(name: String, player: OfflinePlayer): EconomyResponse = notSupported()
    override fun isBankMember(name: String, playerName: String): EconomyResponse = notSupported()
    override fun isBankMember(name: String, player: OfflinePlayer): EconomyResponse = notSupported()
    override fun getBanks(): List<String> = emptyList()

    private fun notSupported(): EconomyResponse =
        EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported")
}

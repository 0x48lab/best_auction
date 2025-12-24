package com.hacklab.best_auction.economy

import com.hacklab.best_auction.Main
import com.hacklab.best_auction.database.PlayerBalances
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.text.NumberFormat
import java.time.LocalDateTime
import java.util.*

/**
 * 内蔵経済システム
 * SQLiteに残高を保存して管理する
 */
class InternalEconomy(private val plugin: Main) : EconomyProvider {

    override val name: String = "BestAuction Internal Economy"

    private val currencyName: String
        get() = plugin.config.getString("currency.fallback_currency", "gil") ?: "gil"

    private val startingBalance: Long
        get() = plugin.config.getLong("economy.internal.starting_balance", 1000)

    override fun currencyNamePlural(): String = currencyName

    override fun format(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale.getDefault())
        return "${formatter.format(amount.toLong())} $currencyName"
    }

    override fun has(player: Player, amount: Double): Boolean {
        return getBalance(player) >= amount
    }

    override fun has(player: OfflinePlayer, amount: Double): Boolean {
        return getBalance(player) >= amount
    }

    override fun hasAccount(player: Player): Boolean {
        return transaction {
            PlayerBalances.select { PlayerBalances.playerUuid eq player.uniqueId.toString() }.count() > 0
        }
    }

    override fun hasAccount(player: OfflinePlayer): Boolean {
        return transaction {
            PlayerBalances.select { PlayerBalances.playerUuid eq player.uniqueId.toString() }.count() > 0
        }
    }

    override fun withdrawPlayer(player: Player, amount: Double): Boolean {
        return withdrawPlayer(player as OfflinePlayer, amount)
    }

    override fun withdrawPlayer(player: OfflinePlayer, amount: Double): Boolean {
        val uuid = player.uniqueId.toString()
        val amountLong = amount.toLong()

        return transaction {
            ensureAccount(player)

            val currentBalance = PlayerBalances.select { PlayerBalances.playerUuid eq uuid }
                .map { it[PlayerBalances.balance] }
                .firstOrNull() ?: 0L

            if (currentBalance < amountLong) {
                return@transaction false
            }

            PlayerBalances.update({ PlayerBalances.playerUuid eq uuid }) {
                it[balance] = currentBalance - amountLong
                it[updatedAt] = LocalDateTime.now()
            }
            true
        }
    }

    override fun depositPlayer(player: Player, amount: Double): Boolean {
        return depositPlayer(player as OfflinePlayer, amount)
    }

    override fun depositPlayer(player: OfflinePlayer, amount: Double): Boolean {
        val uuid = player.uniqueId.toString()
        val amountLong = amount.toLong()

        return transaction {
            ensureAccount(player)

            val currentBalance = PlayerBalances.select { PlayerBalances.playerUuid eq uuid }
                .map { it[PlayerBalances.balance] }
                .firstOrNull() ?: 0L

            PlayerBalances.update({ PlayerBalances.playerUuid eq uuid }) {
                it[balance] = currentBalance + amountLong
                it[updatedAt] = LocalDateTime.now()
            }
            true
        }
    }

    override fun getBalance(player: Player): Double {
        return getBalance(player as OfflinePlayer)
    }

    override fun getBalance(player: OfflinePlayer): Double {
        val uuid = player.uniqueId.toString()

        return transaction {
            ensureAccount(player)

            PlayerBalances.select { PlayerBalances.playerUuid eq uuid }
                .map { it[PlayerBalances.balance].toDouble() }
                .firstOrNull() ?: startingBalance.toDouble()
        }
    }

    /**
     * プレイヤーの残高を設定する（管理者用）
     */
    fun setBalance(player: OfflinePlayer, amount: Long): Boolean {
        val uuid = player.uniqueId.toString()

        return transaction {
            ensureAccount(player)

            PlayerBalances.update({ PlayerBalances.playerUuid eq uuid }) {
                it[balance] = amount
                it[updatedAt] = LocalDateTime.now()
            }
            true
        }
    }

    /**
     * アカウントが存在しない場合は作成する
     */
    private fun ensureAccount(player: OfflinePlayer) {
        val uuid = player.uniqueId.toString()
        val playerName = player.name ?: "Unknown"

        val exists = PlayerBalances.select { PlayerBalances.playerUuid eq uuid }.count() > 0

        if (!exists) {
            PlayerBalances.insert {
                it[playerUuid] = uuid
                it[PlayerBalances.playerName] = playerName
                it[balance] = startingBalance
                it[updatedAt] = LocalDateTime.now()
            }
            plugin.logger.info("Created internal economy account for $playerName with starting balance: $startingBalance")
        }
    }
}

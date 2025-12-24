package com.hacklab.best_auction.economy

import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

/**
 * 経済システムの抽象インターフェース
 * 内蔵経済またはVault経済のどちらでも使用可能
 */
interface EconomyProvider {
    /**
     * 経済プロバイダーの名前
     */
    val name: String

    /**
     * 通貨名（複数形）
     */
    fun currencyNamePlural(): String

    /**
     * 金額をフォーマットして表示用文字列を返す
     */
    fun format(amount: Double): String

    /**
     * プレイヤーが指定金額以上の残高を持っているか確認
     */
    fun has(player: Player, amount: Double): Boolean

    /**
     * オフラインプレイヤーが指定金額以上の残高を持っているか確認
     */
    fun has(player: OfflinePlayer, amount: Double): Boolean

    /**
     * プレイヤーのアカウントが存在するか確認
     */
    fun hasAccount(player: Player): Boolean

    /**
     * オフラインプレイヤーのアカウントが存在するか確認
     */
    fun hasAccount(player: OfflinePlayer): Boolean

    /**
     * プレイヤーから金額を引き出す
     * @return 成功した場合true
     */
    fun withdrawPlayer(player: Player, amount: Double): Boolean

    /**
     * オフラインプレイヤーから金額を引き出す
     * @return 成功した場合true
     */
    fun withdrawPlayer(player: OfflinePlayer, amount: Double): Boolean

    /**
     * プレイヤーに金額を入金する
     * @return 成功した場合true
     */
    fun depositPlayer(player: Player, amount: Double): Boolean

    /**
     * オフラインプレイヤーに金額を入金する
     * @return 成功した場合true
     */
    fun depositPlayer(player: OfflinePlayer, amount: Double): Boolean

    /**
     * プレイヤーの残高を取得
     */
    fun getBalance(player: Player): Double

    /**
     * オフラインプレイヤーの残高を取得
     */
    fun getBalance(player: OfflinePlayer): Double
}

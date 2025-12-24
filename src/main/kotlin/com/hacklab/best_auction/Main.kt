package com.hacklab.best_auction

import com.hacklab.best_auction.commands.AuctionCommand
import com.hacklab.best_auction.database.DatabaseManager
import com.hacklab.best_auction.economy.EconomyProvider
import com.hacklab.best_auction.economy.InternalEconomy
import com.hacklab.best_auction.economy.VaultEconomy
import com.hacklab.best_auction.economy.VaultEconomyHook
import org.bukkit.plugin.ServicePriority
import com.hacklab.best_auction.handlers.BidHandler
import com.hacklab.best_auction.handlers.SearchHandler
import com.hacklab.best_auction.managers.AuctionManager
import com.hacklab.best_auction.managers.CloudEventManager
import com.hacklab.best_auction.managers.MailManager
import com.hacklab.best_auction.tasks.ExpirationTask
import com.hacklab.best_auction.ui.AuctionUI
import com.hacklab.best_auction.utils.LangManager
import net.milkbowl.vault.economy.Economy
import org.bukkit.plugin.java.JavaPlugin

class Main : JavaPlugin() {

    companion object {
        lateinit var instance: Main
            private set
    }

    private lateinit var economyProvider: EconomyProvider
    lateinit var auctionManager: AuctionManager
    lateinit var mailManager: MailManager
    lateinit var cloudEventManager: CloudEventManager
    lateinit var bidHandler: BidHandler
    lateinit var searchHandler: SearchHandler
    private lateinit var databaseManager: DatabaseManager
    lateinit var langManager: LangManager

    override fun onEnable() {
        instance = this

        // Save default config
        saveDefaultConfig()

        // Initialize language manager
        langManager = LangManager(this)

        // Initialize database first (needed for internal economy)
        databaseManager = DatabaseManager(dataFolder)
        databaseManager.init()

        // Setup economy
        if (!setupEconomy()) {
            server.pluginManager.disablePlugin(this)
            return
        }

        mailManager = MailManager(this)
        cloudEventManager = CloudEventManager(this)
        auctionManager = AuctionManager(this, economyProvider, cloudEventManager)
        bidHandler = BidHandler(this)
        searchHandler = SearchHandler(this)

        getCommand("auction")?.setExecutor(AuctionCommand(this))

        server.pluginManager.registerEvents(AuctionUI(), this)
        server.pluginManager.registerEvents(bidHandler, this)
        server.pluginManager.registerEvents(searchHandler, this)
        server.pluginManager.registerEvents(com.hacklab.best_auction.ui.LanguageSettingsUI(), this)

        ExpirationTask(this).runTaskTimer(this, 20L * 60L * 5L, 20L * 60L * 5L)

        logger.info(langManager.getMessage("general.enabled"))
    }

    override fun onDisable() {
        if (::cloudEventManager.isInitialized) {
            cloudEventManager.shutdown()
        }
        logger.info(langManager.getMessage("general.disabled"))
    }

    private fun setupEconomy(): Boolean {
        val providerType = config.getString("economy.provider", "auto") ?: "auto"

        when (providerType.lowercase()) {
            "internal" -> {
                // 強制的に内蔵経済を使用
                val internalEconomy = InternalEconomy(this)
                economyProvider = internalEconomy
                registerInternalEconomyToVault(internalEconomy)
                logger.info(langManager.getMessage("general.economy_internal"))
                return true
            }
            "vault" -> {
                // Vaultのみを使用（見つからない場合は失敗）
                val vaultEconomy = trySetupVaultEconomy()
                if (vaultEconomy == null) {
                    logger.severe(langManager.getMessage("general.vault_not_found"))
                    return false
                }
                economyProvider = vaultEconomy
                return true
            }
            else -> {
                // auto: Vaultが利用可能なら使用、そうでなければ内蔵経済
                val vaultEconomy = trySetupVaultEconomy()
                if (vaultEconomy != null) {
                    economyProvider = vaultEconomy
                } else {
                    val internalEconomy = InternalEconomy(this)
                    economyProvider = internalEconomy
                    registerInternalEconomyToVault(internalEconomy)
                    logger.info(langManager.getMessage("general.economy_internal"))
                }
                return true
            }
        }
    }

    /**
     * 内蔵経済システムをVaultに登録して他のプラグインから使用可能にする
     */
    private fun registerInternalEconomyToVault(internalEconomy: InternalEconomy) {
        // VaultまたはVaultUnlockedが存在する場合のみ登録
        val vaultPlugin = server.pluginManager.getPlugin("Vault")
            ?: server.pluginManager.getPlugin("VaultUnlocked")

        if (vaultPlugin != null) {
            val hook = VaultEconomyHook(this, internalEconomy)
            server.servicesManager.register(Economy::class.java, hook, this, ServicePriority.Normal)
            logger.info(langManager.getMessage("general.economy_registered"))
        }
    }

    /**
     * Vault経済システムのセットアップを試みる
     * @return 成功した場合はVaultEconomy、失敗した場合はnull
     */
    private fun trySetupVaultEconomy(): VaultEconomy? {
        // VaultまたはVaultUnlockedをチェック
        val vaultPlugin = server.pluginManager.getPlugin("Vault")
            ?: server.pluginManager.getPlugin("VaultUnlocked")

        if (vaultPlugin == null) {
            return null
        }

        logger.info("Vault API detected: ${vaultPlugin.name} v${vaultPlugin.description.version}")

        val rsp = server.servicesManager.getRegistration(Economy::class.java)
        if (rsp == null) {
            logger.info(langManager.getMessage("general.no_economy_fallback"))
            return null
        }

        val vaultEconomy = VaultEconomy(rsp.provider)
        logger.info(langManager.getMessage("general.economy_found", vaultEconomy.name))
        return vaultEconomy
    }

    fun getEconomyProvider(): EconomyProvider? = if (::economyProvider.isInitialized) economyProvider else null

    /**
     * 内蔵経済システムを取得（内蔵経済使用時のみ）
     */
    fun getInternalEconomy(): InternalEconomy? {
        return economyProvider as? InternalEconomy
    }
}

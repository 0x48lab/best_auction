package com.hacklab.best_auction

import com.hacklab.best_auction.commands.AuctionCommand
import com.hacklab.best_auction.database.DatabaseManager
import com.hacklab.best_auction.handlers.BidHandler
import com.hacklab.best_auction.handlers.SearchHandler
import com.hacklab.best_auction.managers.AuctionManager
import com.hacklab.best_auction.managers.CloudEventManager
import com.hacklab.best_auction.managers.MailManager
import com.hacklab.best_auction.tasks.ExpirationTask
import com.hacklab.best_auction.ui.AuctionUI
import com.hacklab.best_auction.utils.LangManager
import net.milkbowl.vault.economy.Economy
import org.bukkit.plugin.RegisteredServiceProvider
import org.bukkit.plugin.java.JavaPlugin

class Main : JavaPlugin() {

    companion object {
        lateinit var instance: Main
            private set
    }

    private lateinit var economy: Economy
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

        if (!setupEconomy()) {
            logger.severe(langManager.getMessage("general.vault_not_found"))
            server.pluginManager.disablePlugin(this)
            return
        }

        databaseManager = DatabaseManager(dataFolder)
        databaseManager.init()

        mailManager = MailManager(this)
        cloudEventManager = CloudEventManager(this)
        auctionManager = AuctionManager(this, economy, cloudEventManager)
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
        if (server.pluginManager.getPlugin("Vault") == null) {
            return false
        }

        val rsp = server.servicesManager.getRegistration(Economy::class.java)
        if (rsp == null) {
            logger.severe(langManager.getMessage("general.no_economy"))
            return false
        }

        economy = rsp.provider
        logger.info(langManager.getMessage("general.economy_found", economy.name))
        return true
    }
    
    fun getEconomy(): Economy? = if (::economy.isInitialized) economy else null
}

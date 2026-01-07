package com.hacklab.best_auction

import com.hacklab.best_auction.commands.AuctionCommand
import com.hacklab.best_auction.commands.EconomyCommand
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
import org.bukkit.command.PluginCommand
import org.bukkit.plugin.java.JavaPlugin
import java.lang.reflect.Constructor

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

        // Register shortcut economy commands if using internal economy
        registerShortcutCommands()

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

    /**
     * ショートカット経済コマンドを登録
     * 内蔵経済使用時、かつ設定で有効な場合のみ登録
     */
    private fun registerShortcutCommands() {
        // 内蔵経済を使用していない場合は登録しない
        if (economyProvider !is InternalEconomy) {
            logger.info("External economy detected, skipping shortcut command registration")
            return
        }

        // 設定で無効になっている場合は登録しない
        if (!config.getBoolean("economy.register_shortcut_commands", true)) {
            logger.info("Shortcut command registration disabled in config")
            return
        }

        try {
            val commandMap = server.commandMap

            // /balance, /bal, /money コマンド
            val balanceCommand = EconomyCommand(this, EconomyCommand.CommandType.BALANCE)
            registerCommand("balance", balanceCommand, "Check your balance", listOf("bal", "money"))

            // /pay コマンド
            val payCommand = EconomyCommand(this, EconomyCommand.CommandType.PAY)
            registerCommand("pay", payCommand, "Send money to another player", listOf("send"))

            logger.info("Registered shortcut economy commands: /balance, /bal, /money, /pay, /send")
        } catch (e: Exception) {
            logger.warning("Failed to register shortcut economy commands: ${e.message}")
        }
    }

    /**
     * コマンドを動的に登録
     */
    private fun registerCommand(name: String, executor: EconomyCommand, description: String, aliases: List<String> = emptyList()) {
        try {
            val commandMap = server.commandMap

            // PluginCommandのコンストラクタを取得（privateなのでリフレクション使用）
            val constructor: Constructor<PluginCommand> = PluginCommand::class.java.getDeclaredConstructor(
                String::class.java,
                org.bukkit.plugin.Plugin::class.java
            )
            constructor.isAccessible = true

            val command = constructor.newInstance(name, this)
            command.description = description
            command.setExecutor(executor)
            command.tabCompleter = executor
            command.aliases = aliases

            // 既存のコマンドがある場合は登録をスキップ
            val existingCommand = commandMap.getCommand(name)
            if (existingCommand != null) {
                val pluginName = if (existingCommand is PluginCommand) {
                    existingCommand.plugin?.name ?: "unknown"
                } else {
                    "unknown"
                }
                if (existingCommand !is PluginCommand || existingCommand.plugin != this) {
                    logger.info("Command /$name already registered by $pluginName, skipping")
                    return
                }
            }

            commandMap.register(this.name.lowercase(), command)
        } catch (e: Exception) {
            logger.warning("Failed to register command /$name: ${e.message}")
        }
    }
}

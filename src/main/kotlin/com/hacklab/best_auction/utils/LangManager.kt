package com.hacklab.best_auction.utils

import com.hacklab.best_auction.Main
import com.hacklab.best_auction.database.PlayerLanguageSettings
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.io.InputStreamReader
import java.text.MessageFormat
import java.time.LocalDateTime
import java.util.*

class LangManager(private val plugin: Main) {
    
    private val languages = mutableMapOf<String, YamlConfiguration>()
    private var defaultLanguage = "en"
    
    init {
        loadLanguages()
    }
    
    private fun loadLanguages() {
        val langDir = File(plugin.dataFolder, "lang")
        if (!langDir.exists()) {
            langDir.mkdirs()
        }
        
        // Load default languages from resources
        val defaultLangs = listOf("en", "ja")
        for (lang in defaultLangs) {
            val langFile = File(langDir, "$lang.yml")
            if (!langFile.exists()) {
                plugin.saveResource("lang/$lang.yml", false)
            }
            
            try {
                val config = YamlConfiguration.loadConfiguration(langFile)
                // Load defaults from resources
                val defaultStream = plugin.getResource("lang/$lang.yml")
                if (defaultStream != null) {
                    val defaultConfig = YamlConfiguration.loadConfiguration(InputStreamReader(defaultStream, "UTF-8"))
                    config.setDefaults(defaultConfig)
                }
                languages[lang] = config
                plugin.logger.info("Loaded language: $lang")
            } catch (e: Exception) {
                plugin.logger.warning("Failed to load language file: $lang.yml - ${e.message}")
            }
        }
        
        // Set default language
        val configLang = plugin.config.getString("language", "en") ?: "en"
        if (languages.containsKey(configLang)) {
            defaultLanguage = configLang
        }
        
        plugin.logger.info("Default language set to: $defaultLanguage")
    }
    
    fun getMessage(key: String, vararg args: Any): String {
        return getMessage(defaultLanguage, key, *args)
    }
    
    fun getMessage(player: Player, key: String, vararg args: Any): String {
        val playerLang = getPlayerLanguage(player)
        return getMessage(playerLang, key, *args)
    }
    
    fun getMessage(lang: String, key: String, vararg args: Any): String {
        val config = languages[lang] ?: languages[defaultLanguage]
        
        val message = config?.getString(key) ?: run {
            // Fallback to default language if key not found
            if (lang != defaultLanguage) {
                languages[defaultLanguage]?.getString(key)
            } else {
                null
            }
        } ?: run {
            plugin.logger.warning("Missing translation key: $key in language: $lang (available languages: ${languages.keys})")
            plugin.logger.warning("Config keys for $lang: ${config?.getKeys(true)?.take(10)}")
            "§c[Missing: $key]"
        }
        
        return if (args.isNotEmpty()) {
            try {
                MessageFormat.format(message, *args)
            } catch (e: Exception) {
                plugin.logger.warning("Failed to format message: $key - ${e.message}")
                message
            }
        } else {
            message
        }
    }
    
    private fun getPlayerLanguage(player: Player): String {
        return transaction {
            // Check for player-specific language setting
            val setting = PlayerLanguageSettings.select { 
                PlayerLanguageSettings.playerUuid eq player.uniqueId.toString() 
            }.singleOrNull()
            
            if (setting != null) {
                val savedLang = setting[PlayerLanguageSettings.language]
                if (savedLang != "auto" && languages.containsKey(savedLang)) {
                    return@transaction savedLang
                }
            }
            
            // Auto-detect from client locale
            try {
                val locale = player.locale
                when {
                    locale.startsWith("ja") -> "ja"
                    locale.startsWith("en") -> "en"
                    else -> defaultLanguage
                }
            } catch (e: Exception) {
                defaultLanguage
            }
        }
    }
    
    fun setPlayerLanguage(player: Player, language: String): Boolean {
        if (!languages.containsKey(language) && language != "auto") {
            return false
        }
        
        return transaction {
            try {
                val existing = PlayerLanguageSettings.select { 
                    PlayerLanguageSettings.playerUuid eq player.uniqueId.toString() 
                }.singleOrNull()
                
                if (existing != null) {
                    PlayerLanguageSettings.update({ 
                        PlayerLanguageSettings.playerUuid eq player.uniqueId.toString() 
                    }) {
                        it[PlayerLanguageSettings.language] = language
                        it[updatedAt] = LocalDateTime.now()
                    }
                } else {
                    PlayerLanguageSettings.insert {
                        it[playerUuid] = player.uniqueId.toString()
                        it[PlayerLanguageSettings.language] = language
                    }
                }
                true
            } catch (e: Exception) {
                plugin.logger.warning("Failed to save player language setting: ${e.message}")
                false
            }
        }
    }
    
    fun getPlayerLanguageSetting(player: Player): String {
        return transaction {
            PlayerLanguageSettings.select { 
                PlayerLanguageSettings.playerUuid eq player.uniqueId.toString() 
            }.singleOrNull()?.get(PlayerLanguageSettings.language) ?: "auto"
        }
    }
    
    fun getAvailableLanguages(): Set<String> {
        return languages.keys
    }
    
    fun reloadLanguages() {
        languages.clear()
        loadLanguages()
    }
    
    fun setDefaultLanguage(lang: String) {
        if (languages.containsKey(lang)) {
            defaultLanguage = lang
            plugin.config.set("language", lang)
            plugin.saveConfig()
        }
    }
    
    // Utility methods for common message types
    fun sendMessage(player: Player, key: String, vararg args: Any) {
        player.sendMessage(getMessage(player, key, *args))
    }
    
    fun sendErrorMessage(player: Player, key: String, vararg args: Any) {
        player.sendMessage("§c" + getMessage(player, key, *args))
    }
    
    fun sendSuccessMessage(player: Player, key: String, vararg args: Any) {
        player.sendMessage("§a" + getMessage(player, key, *args))
    }
    
    fun sendInfoMessage(player: Player, key: String, vararg args: Any) {
        player.sendMessage("§e" + getMessage(player, key, *args))
    }
}
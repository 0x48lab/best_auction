package com.hacklab.best_auction.database

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

class DatabaseManager(private val dataFolder: File) {
    
    fun init() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }
        
        val dbFile = File(dataFolder, "auction.db")
        Database.connect("jdbc:sqlite:${dbFile.absolutePath}", "org.sqlite.JDBC")
        
        transaction {
            SchemaUtils.create(AuctionItems, Bids, MailBox, AuctionSettings, PlayerLanguageSettings, CloudSyncStatus)
        }
    }
}
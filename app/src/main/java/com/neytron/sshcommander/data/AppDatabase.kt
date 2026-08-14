package com.neytron.sshcommander.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Server::class, CustomCommand::class, CommandHistoryEntity::class, ServerLogin::class], version = 8, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE servers ADD COLUMN privateKeyPath TEXT")
                db.execSQL("ALTER TABLE servers ADD COLUMN passphraseKey TEXT")
                db.execSQL("ALTER TABLE servers ADD COLUMN hostKey TEXT")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE servers ADD COLUMN hostKeyType TEXT")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE servers ADD COLUMN widgetCommand TEXT")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE servers ADD COLUMN sftpStartPath TEXT")
                db.execSQL("ALTER TABLE servers ADD COLUMN lastSftpPath TEXT")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS server_logins (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "serverId INTEGER NOT NULL, " +
                        "label TEXT NOT NULL, " +
                        "username TEXT NOT NULL, " +
                        "passwordKey TEXT NOT NULL DEFAULT '', " +
                        "privateKeyPath TEXT, " +
                        "passphraseKey TEXT, " +
                        "sftpStartPath TEXT, " +
                        "lastSftpPath TEXT, " +
                        "isDefault INTEGER NOT NULL DEFAULT 0)"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ssh_commander_db"
                )
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

package com.example.prayerapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Prayer::class, Category::class], version = 3, exportSchema = false)
abstract class PrayerDatabase : RoomDatabase() {
    abstract fun prayerDao(): PrayerDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: PrayerDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE prayers ADD COLUMN category TEXT NOT NULL DEFAULT 'Catholic'")
                db.execSQL("ALTER TABLE prayers ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create categories table
                db.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `position` INTEGER NOT NULL DEFAULT 0)")

                // Add categoryId to prayers
                db.execSQL("ALTER TABLE prayers ADD COLUMN categoryId INTEGER NOT NULL DEFAULT 0")

                // For existing users, create "Catholic" and "Umbanda" if they had data, or just migrate them to ID 1 and 2
                // Since we want to start from scratch for new users but not lose data for old ones:
                db.execSQL("INSERT INTO categories (id, name, position) VALUES (1, 'Catholic', 0)")
                db.execSQL("INSERT INTO categories (id, name, position) VALUES (2, 'Umbanda', 1)")

                db.execSQL("UPDATE prayers SET categoryId = 1 WHERE category = 'Catholic'")
                db.execSQL("UPDATE prayers SET categoryId = 2 WHERE category = 'Umbanda'")
            }
        }

        fun getDatabase(context: Context): PrayerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PrayerDatabase::class.java,
                    "prayer_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

package com.example.prayerapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Prayer::class, Category::class, StoicMeditation::class], version = 4, exportSchema = false)
abstract class PrayerDatabase : RoomDatabase() {
    abstract fun prayerDao(): PrayerDao
    abstract fun categoryDao(): CategoryDao
    abstract fun stoicDao(): StoicDao

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
                db.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `position` INTEGER NOT NULL DEFAULT 0)")
                db.execSQL("CREATE TABLE `prayers_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `categoryId` INTEGER NOT NULL DEFAULT 0, `position` INTEGER NOT NULL DEFAULT 0)")
                db.execSQL("INSERT INTO `prayers_new` (id, title, content, position) SELECT id, title, content, position FROM prayers")
                db.execSQL("INSERT INTO categories (id, name, position) VALUES (1, 'Catholic', 0)")
                db.execSQL("INSERT INTO categories (id, name, position) VALUES (2, 'Umbanda', 1)")
                db.execSQL("UPDATE `prayers_new` SET categoryId = 1 WHERE id IN (SELECT id FROM prayers WHERE category = 'Catholic')")
                db.execSQL("UPDATE `prayers_new` SET categoryId = 2 WHERE id IN (SELECT id FROM prayers WHERE category = 'Umbanda')")
                db.execSQL("DROP TABLE prayers")
                db.execSQL("ALTER TABLE prayers_new RENAME TO prayers")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `stoic_meditations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `month` INTEGER NOT NULL, `day` INTEGER NOT NULL, `title` TEXT NOT NULL, `quote` TEXT NOT NULL, `source` TEXT NOT NULL, `commentary` TEXT NOT NULL, `isRead` INTEGER NOT NULL DEFAULT 0)")
            }
        }

        fun getDatabase(context: Context): PrayerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PrayerDatabase::class.java,
                    "prayer_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

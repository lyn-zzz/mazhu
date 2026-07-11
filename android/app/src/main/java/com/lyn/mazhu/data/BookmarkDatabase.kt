package com.lyn.mazhu.data

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Bookmark::class, Collection::class, PendingDeletion::class],
    version = 4,
    exportSchema = false,
)
abstract class BookmarkDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        fun create(context: Context): BookmarkDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                BookmarkDatabase::class.java,
                "mazhu.db",
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bookmarks ADD COLUMN accountName TEXT")
                db.execSQL("ALTER TABLE bookmarks ADD COLUMN coverUrl TEXT")
                db.execSQL("ALTER TABLE bookmarks ADD COLUMN publishedAt INTEGER")
                db.execSQL("ALTER TABLE bookmarks ADD COLUMN contentText TEXT")
                db.execSQL("ALTER TABLE bookmarks ADD COLUMN parseError TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS collections (
                        id TEXT NOT NULL,
                        name TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_collections_name
                    ON collections(name)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO collections(id, name, createdAt)
                    VALUES('default', '默认收藏夹', 0)
                    """.trimIndent(),
                )

                db.execSQL(
                    """
                    CREATE TABLE bookmarks_new (
                        id TEXT NOT NULL,
                        originalUrl TEXT NOT NULL,
                        normalizedUrl TEXT NOT NULL,
                        title TEXT NOT NULL,
                        accountName TEXT,
                        coverUrl TEXT,
                        publishedAt INTEGER,
                        contentText TEXT,
                        collectionId TEXT NOT NULL,
                        parseStatus TEXT NOT NULL,
                        parseError TEXT,
                        syncStatus TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO bookmarks_new(
                        id,
                        originalUrl,
                        normalizedUrl,
                        title,
                        accountName,
                        coverUrl,
                        publishedAt,
                        contentText,
                        collectionId,
                        parseStatus,
                        parseError,
                        syncStatus,
                        createdAt
                    )
                    SELECT
                        id,
                        originalUrl,
                        normalizedUrl,
                        title,
                        accountName,
                        coverUrl,
                        publishedAt,
                        contentText,
                        'default',
                        parseStatus,
                        parseError,
                        syncStatus,
                        createdAt
                    FROM bookmarks
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE bookmarks")
                db.execSQL("ALTER TABLE bookmarks_new RENAME TO bookmarks")
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX index_bookmarks_normalizedUrl
                    ON bookmarks(normalizedUrl)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE INDEX index_bookmarks_collectionId
                    ON bookmarks(collectionId)
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE collections
                    ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'local_only'
                    """.trimIndent(),
                )
                db.execSQL("ALTER TABLE collections ADD COLUMN syncError TEXT")
                db.execSQL("ALTER TABLE bookmarks ADD COLUMN syncError TEXT")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pending_deletions (
                        key TEXT NOT NULL,
                        entityType TEXT NOT NULL,
                        entityId TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        PRIMARY KEY(key)
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}

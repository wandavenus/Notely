package dev.wndavenz.notely.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FolderEntity::class, NoteEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class NotelyDatabase : RoomDatabase() {

    abstract fun folderDao(): FolderDao

    abstract fun noteDao(): NoteDao

    companion object {

        /** v2: kolom secret untuk brankas catatan rahasia. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE notes ADD COLUMN secret INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        @Volatile
        private var instance: NotelyDatabase? = null

        fun getInstance(context: Context): NotelyDatabase {
            return instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }
        }

        private fun build(context: Context): NotelyDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                NotelyDatabase::class.java,
                "notely.db",
            ).addMigrations(MIGRATION_1_2).addCallback(object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    val now = System.currentTimeMillis()
                    db.execSQL(
                        "INSERT INTO folders (name, createdAt) VALUES ('Catatan', $now)",
                    )
                    db.execSQL(
                        "INSERT INTO notes (folderId, body, pinned, createdAt, updatedAt) " +
                            "VALUES (1, 'Selamat datang di Notely\n" +
                            "Ketuk tombol pena di kanan bawah untuk membuat catatan baru. " +
                            "Tekan lama sebuah catatan untuk menyematkan atau menghapusnya.', " +
                            "1, $now, $now)",
                    )
                }
            }).build()
        }
    }
}

package dev.wndavenz.notely.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FolderEntity::class, NoteEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class NotelyDatabase : RoomDatabase() {

    abstract fun folderDao(): FolderDao

    abstract fun noteDao(): NoteDao

    companion object {
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
            ).addCallback(object : Callback() {
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

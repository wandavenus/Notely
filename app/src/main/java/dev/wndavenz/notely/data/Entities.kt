package dev.wndavenz.notely.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Folder catatan. Folder pertama (id terkecil) adalah folder bawaan "Catatan". */
@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

/** Satu catatan. Judul dan pratinjau diturunkan dari [body] (baris pertama = judul). */
@Entity(
    tableName = "notes",
    indices = [Index(value = ["folderId"])],
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: Long = 1,
    val body: String = "",
    val pinned: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

/** Folder beserta jumlah catatan aktif (bukan sampah) di dalamnya. */
data class FolderSummary(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val noteCount: Int,
)

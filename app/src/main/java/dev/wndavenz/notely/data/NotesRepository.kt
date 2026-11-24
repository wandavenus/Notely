package dev.wndavenz.notely.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

/**
 * Satu-satunya pintu akses data untuk lapisan UI.
 * Semua daftar diekspos sebagai [Flow] agar UI selalu mengikuti isi database.
 */
class NotesRepository(private val database: NotelyDatabase) {

    private val folderDao = database.folderDao()
    private val noteDao = database.noteDao()

    fun folders(): Flow<List<FolderSummary>> = folderDao.observeFolders()

    suspend fun firstFolderId(): Long = folderDao.firstFolder()?.id ?: 1L

    suspend fun createFolder(name: String): Long {
        return folderDao.insert(
            FolderEntity(name = name, createdAt = System.currentTimeMillis()),
        )
    }

    /** Menghapus folder buatan pengguna; catatannya dipindah ke folder bawaan. */
    suspend fun deleteFolder(folderId: Long) {
        val fallbackId = firstFolderId()
        if (fallbackId == folderId) return
        database.withTransaction {
            noteDao.moveAllNotes(fromFolder = folderId, toFolder = fallbackId)
            folderDao.deleteById(folderId)
        }
    }

    fun notesInFolder(folderId: Long): Flow<List<NoteEntity>> =
        noteDao.observeInFolder(folderId)

    fun pinnedNotes(): Flow<List<NoteEntity>> = noteDao.observePinned()

    fun pinnedCount(): Flow<Int> = noteDao.observePinnedCount()

    fun trashedNotes(): Flow<List<NoteEntity>> = noteDao.observeTrash()

    fun trashCount(): Flow<Int> = noteDao.observeTrashCount()

    fun searchNotes(query: String): Flow<List<NoteEntity>> = noteDao.search(query)

    fun secretNotes(): Flow<List<NoteEntity>> = noteDao.observeSecret()

    fun secretTrashedNotes(): Flow<List<NoteEntity>> = noteDao.observeSecretTrash()

    suspend fun setSecret(id: Long, secret: Boolean) = noteDao.setSecret(id, secret)

    suspend fun emptySecretTrash() = noteDao.emptySecretTrash()

    suspend fun noteById(id: Long): NoteEntity? = noteDao.getById(id)

    suspend fun createNote(folderId: Long, body: String, secret: Boolean = false): Long {
        val now = System.currentTimeMillis()
        return noteDao.insert(
            NoteEntity(
                folderId = folderId,
                body = body,
                createdAt = now,
                updatedAt = now,
                secret = secret,
            ),
        )
    }

    suspend fun updateNoteBody(id: Long, body: String) {
        noteDao.updateBody(id, body, System.currentTimeMillis())
    }

    suspend fun setPinned(id: Long, pinned: Boolean) = noteDao.setPinned(id, pinned)

    suspend fun moveToTrash(id: Long) =
        noteDao.moveToTrash(id, System.currentTimeMillis())

    suspend fun restoreFromTrash(id: Long) = noteDao.restore(id)

    suspend fun deleteForever(id: Long) = noteDao.deleteForever(id)

    suspend fun emptyTrash() = noteDao.emptyTrash()
}

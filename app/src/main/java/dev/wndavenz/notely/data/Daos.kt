package dev.wndavenz.notely.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Query(
        """
        SELECT f.id, f.name, f.createdAt,
               (SELECT COUNT(*) FROM notes n
                 WHERE n.folderId = f.id AND n.deletedAt IS NULL) AS noteCount
        FROM folders f
        ORDER BY f.id ASC
        """,
    )
    fun observeFolders(): Flow<List<FolderSummary>>

    @Insert
    suspend fun insert(folder: FolderEntity): Long

    @Query("SELECT * FROM folders ORDER BY id ASC LIMIT 1")
    suspend fun firstFolder(): FolderEntity?

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface NoteDao {

    @Query(
        "SELECT * FROM notes WHERE folderId = :folderId AND deletedAt IS NULL " +
            "ORDER BY pinned DESC, updatedAt DESC",
    )
    fun observeInFolder(folderId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE pinned = 1 AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observePinned(): Flow<List<NoteEntity>>

    @Query("SELECT COUNT(*) FROM notes WHERE pinned = 1 AND deletedAt IS NULL")
    fun observePinnedCount(): Flow<Int>

    @Query("SELECT * FROM notes WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeTrash(): Flow<List<NoteEntity>>

    @Query("SELECT COUNT(*) FROM notes WHERE deletedAt IS NOT NULL")
    fun observeTrashCount(): Flow<Int>

    @Query(
        "SELECT * FROM notes WHERE deletedAt IS NULL AND body LIKE '%' || :query || '%' " +
            "ORDER BY pinned DESC, updatedAt DESC",
    )
    fun search(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): NoteEntity?

    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Query("UPDATE notes SET body = :body, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateBody(id: Long, body: String, updatedAt: Long)

    @Query("UPDATE notes SET pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("UPDATE notes SET deletedAt = :deletedAt, pinned = 0 WHERE id = :id")
    suspend fun moveToTrash(id: Long, deletedAt: Long)

    @Query("UPDATE notes SET deletedAt = NULL WHERE id = :id")
    suspend fun restore(id: Long)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteForever(id: Long)

    @Query("DELETE FROM notes WHERE deletedAt IS NOT NULL")
    suspend fun emptyTrash()

    @Query("UPDATE notes SET folderId = :toFolder WHERE folderId = :fromFolder")
    suspend fun moveAllNotes(fromFolder: Long, toFolder: Long)
}

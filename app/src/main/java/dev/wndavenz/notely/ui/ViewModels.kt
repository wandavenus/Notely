package dev.wndavenz.notely.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import dev.wndavenz.notely.data.FolderSummary
import dev.wndavenz.notely.data.NoteEntity
import dev.wndavenz.notely.data.NotesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Pabrik ViewModel sederhana (injeksi manual, tanpa framework DI). */
fun <VM : ViewModel> viewModelFactory(initializer: () -> VM): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return initializer() as T
        }
    }
}

// ------------------------------------------------------------- layar Folder

data class FoldersUiState(
    val folders: List<FolderSummary> = emptyList(),
    val pinnedCount: Int = 0,
    val trashCount: Int = 0,
    val searchResults: List<NoteEntity> = emptyList(),
) {
    /** Folder bawaan selalu baris pertama; dipakai tombol tulis baru. */
    val defaultFolderId: Long get() = folders.firstOrNull()?.id ?: 1L
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FoldersViewModel(private val repository: NotesRepository) : ViewModel() {

    private val query = MutableStateFlow("")

    val uiState: StateFlow<FoldersUiState> = combine(
        repository.folders(),
        repository.pinnedCount(),
        repository.trashCount(),
        query.flatMapLatest { text ->
            if (text.isBlank()) flowOf(emptyList()) else repository.searchNotes(text)
        },
    ) { folders, pinnedCount, trashCount, results ->
        FoldersUiState(
            folders = folders,
            pinnedCount = pinnedCount,
            trashCount = trashCount,
            searchResults = results,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FoldersUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun createFolder(name: String) {
        viewModelScope.launch { repository.createFolder(name) }
    }

    fun deleteFolder(folderId: Long) {
        viewModelScope.launch { repository.deleteFolder(folderId) }
    }
}

// ------------------------------------------------------- layar daftar catatan

class NotesListViewModel(
    private val repository: NotesRepository,
    view: ListView,
) : ViewModel() {

    val notes: StateFlow<List<NoteEntity>> = when (view) {
        is ListView.Folder -> repository.notesInFolder(view.id)
        ListView.Pinned -> repository.pinnedNotes()
        ListView.Trash -> repository.trashedNotes()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setPinned(note: NoteEntity, pinned: Boolean) {
        viewModelScope.launch { repository.setPinned(note.id, pinned) }
    }

    fun moveToTrash(note: NoteEntity) {
        viewModelScope.launch { repository.moveToTrash(note.id) }
    }

    fun restore(note: NoteEntity) {
        viewModelScope.launch { repository.restoreFromTrash(note.id) }
    }

    fun deleteForever(note: NoteEntity) {
        viewModelScope.launch { repository.deleteForever(note.id) }
    }

    fun emptyTrash() {
        viewModelScope.launch { repository.emptyTrash() }
    }
}

// ------------------------------------------------------------- layar editor

data class EditorUiState(
    val loaded: Boolean = false,
    val initialBody: String = "",
    val pinned: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)

class EditorViewModel(
    private val repository: NotesRepository,
    private val appScope: CoroutineScope,
    initialNoteId: Long?,
    private val folderId: Long,
) : ViewModel() {

    private var noteId: Long? = initialNoteId
    private var createdInSession = false
    private var saveJob: Job? = null

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    init {
        if (initialNoteId == null) {
            _state.value = EditorUiState(loaded = true)
        } else {
            viewModelScope.launch {
                val note = repository.noteById(initialNoteId)
                _state.value = EditorUiState(
                    loaded = true,
                    initialBody = note?.body ?: "",
                    pinned = note?.pinned ?: false,
                    updatedAt = note?.updatedAt ?: System.currentTimeMillis(),
                )
            }
        }
    }

    /** Simpan otomatis dengan jeda singkat setelah pengguna berhenti mengetik. */
    fun onBodyChanged(body: String) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(400)
            persist(body)
        }
    }

    /** Simpan terakhir saat editor ditutup; berjalan di cakupan aplikasi. */
    fun saveNow(body: String) {
        saveJob?.cancel()
        appScope.launch { persist(body) }
    }

    private suspend fun persist(body: String) {
        val id = noteId
        when {
            id == null -> {
                if (body.isNotBlank()) {
                    noteId = repository.createNote(folderId, body)
                    createdInSession = true
                    _state.update { it.copy(updatedAt = System.currentTimeMillis()) }
                }
            }
            body.isBlank() && createdInSession -> {
                repository.deleteForever(id)
                noteId = null
                createdInSession = false
            }
            else -> repository.updateNoteBody(id, body)
        }
    }

    fun togglePin() {
        val id = noteId ?: return
        val newValue = !_state.value.pinned
        _state.update { it.copy(pinned = newValue) }
        viewModelScope.launch { repository.setPinned(id, newValue) }
    }

    fun moveToTrash() {
        val id = noteId ?: return
        appScope.launch { repository.moveToTrash(id) }
    }
}

// --------------------------------------------------------- layar Pengaturan

data class SettingsUiState(
    val noteCount: Int = 0,
    val folderCount: Int = 0,
    val pinnedCount: Int = 0,
    val trashCount: Int = 0,
)

class SettingsViewModel(private val repository: NotesRepository) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.folders(),
        repository.pinnedCount(),
        repository.trashCount(),
    ) { folders, pinnedCount, trashCount ->
        SettingsUiState(
            noteCount = folders.sumOf { it.noteCount },
            folderCount = folders.size,
            pinnedCount = pinnedCount,
            trashCount = trashCount,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun emptyTrash() {
        viewModelScope.launch { repository.emptyTrash() }
    }
}

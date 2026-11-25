package dev.wndavenz.notely.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import dev.wndavenz.notely.data.FolderSummary
import dev.wndavenz.notely.data.NoteEntity
import dev.wndavenz.notely.data.NotesRepository
import dev.wndavenz.notely.documents.DocumentFile
import dev.wndavenz.notely.documents.DocumentScanner
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
    /** Jumlah dokumen .md/.txt hasil pindai; null = belum dipindai / belum ada akses. */
    val documentCount: Int? = null,
) {
    /** Folder bawaan selalu baris pertama; dipakai tombol tulis baru. */
    val defaultFolderId: Long get() = folders.firstOrNull()?.id ?: 1L
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FoldersViewModel(private val repository: NotesRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    private val documentCount = MutableStateFlow<Int?>(null)
    private var documentScanJob: Job? = null

    val uiState: StateFlow<FoldersUiState> = combine(
        repository.folders(),
        repository.pinnedCount(),
        repository.trashCount(),
        query.flatMapLatest { text ->
            if (text.isBlank()) flowOf(emptyList()) else repository.searchNotes(text)
        },
        documentCount,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        FoldersUiState(
            folders = values[0] as List<FolderSummary>,
            pinnedCount = values[1] as Int,
            trashCount = values[2] as Int,
            searchResults = values[3] as List<NoteEntity>,
            documentCount = values[4] as Int?,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FoldersUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    /**
     * Pindai dokumen untuk angka pada kartu Dokumen. Hanya berjalan bila
     * akses file sudah diberikan; pemindaian ulang dibatalkan otomatis.
     */
    fun refreshDocumentCount(hasAccess: Boolean) {
        documentScanJob?.cancel()
        if (!hasAccess) {
            documentCount.value = null
            return
        }
        documentScanJob = viewModelScope.launch {
            documentCount.value = DocumentScanner.scan().size
        }
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
        ListView.Secret -> repository.secretNotes()
        ListView.SecretTrash -> repository.secretTrashedNotes()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val isSecretTrash = view is ListView.SecretTrash

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

    fun setSecret(note: NoteEntity, secret: Boolean) {
        viewModelScope.launch { repository.setSecret(note.id, secret) }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            if (isSecretTrash) repository.emptySecretTrash() else repository.emptyTrash()
        }
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
    private val secret: Boolean = false,
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
                    noteId = repository.createNote(folderId, body, secret)
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

// --------------------------------------------------------- layar Dokumen

sealed interface DocumentsUiState {
    data object NeedAccess : DocumentsUiState
    data object Scanning : DocumentsUiState
    data class Ready(val documents: List<DocumentFile>) : DocumentsUiState
}

class DocumentsViewModel : ViewModel() {

    private val _state = MutableStateFlow<DocumentsUiState>(DocumentsUiState.Scanning)
    val state: StateFlow<DocumentsUiState> = _state.asStateFlow()

    private var scanJob: Job? = null

    /** Muat ulang daftar: tanpa akses -> minta izin; dengan akses -> pindai. */
    fun refresh(hasAccess: Boolean) {
        scanJob?.cancel()
        if (!hasAccess) {
            _state.value = DocumentsUiState.NeedAccess
            return
        }
        _state.value = DocumentsUiState.Scanning
        scanJob = viewModelScope.launch {
            _state.value = DocumentsUiState.Ready(DocumentScanner.scan())
        }
    }
}

// ----------------------------------------------------- layar baca dokumen

data class DocumentReaderUiState(
    val loaded: Boolean = false,
    val text: String = "",
    val truncated: Boolean = false,
    val failed: Boolean = false,
)

class DocumentReaderViewModel(private val document: DocumentFile) : ViewModel() {

    private val _state = MutableStateFlow(DocumentReaderUiState())
    val state: StateFlow<DocumentReaderUiState> = _state.asStateFlow()

    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            try {
                val read = DocumentScanner.readText(document)
                _state.value = DocumentReaderUiState(
                    loaded = true,
                    text = read.text,
                    truncated = read.truncated,
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _state.value = DocumentReaderUiState(loaded = true, failed = true)
            }
        }
    }

    /** Mode edit (Hanya Baca mati): simpan otomatis ke file aslinya, jeda 800 ms. */
    fun onEdited(text: String) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(800)
            writeBack(text)
        }
    }

    /** Simpan paksa sekarang (dipakai saat keluar dari mode edit). */
    fun saveNow(text: String) {
        saveJob?.cancel()
        viewModelScope.launch { writeBack(text) }
    }

    private suspend fun writeBack(text: String) {
        try {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                java.io.File(document.path).writeText(text, Charsets.UTF_8)
            }
            _state.update { it.copy(text = text, truncated = false) }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            // Gagal menulis (mis. file dipindah): biarkan teks layar apa adanya;
            // keadaan gagal tidak ditandai supaya pengguna tidak kehilangan edit.
        }
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

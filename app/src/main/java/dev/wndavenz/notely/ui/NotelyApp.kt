package dev.wndavenz.notely.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.wndavenz.notely.data.NoteEntity
import dev.wndavenz.notely.data.NotesRepository
import dev.wndavenz.notely.ui.screens.EditorScreen
import dev.wndavenz.notely.ui.screens.FoldersScreen
import dev.wndavenz.notely.ui.screens.NotesListScreen
import dev.wndavenz.notely.ui.screens.SettingsScreen
import kotlinx.coroutines.CoroutineScope

/** Jenis daftar catatan yang sedang dibuka. */
sealed interface ListView {
    data class Folder(val id: Long, val name: String) : ListView
    data object Pinned : ListView
    data object Trash : ListView
}

fun titleOf(view: ListView): String = when (view) {
    is ListView.Folder -> view.name
    ListView.Pinned -> "Tersimpan"
    ListView.Trash -> "Baru Dihapus"
}

/** Tujuan navigasi aplikasi (tumpukan kembali dikelola manual, satu Activity). */
sealed interface Destination {
    data object Folders : Destination
    data class NoteList(val view: ListView) : Destination
    data class Editor(
        val noteId: Long?,
        val folderId: Long,
        val parentTitle: String,
        val sessionKey: Long = System.nanoTime(),
    ) : Destination
    data object Settings : Destination
}

/** Akar UI Notely: menukar layar teratas dari tumpukan navigasi. */
@Composable
fun NotelyApp(repository: NotesRepository, appScope: CoroutineScope) {
    val backStack = remember { mutableStateListOf<Destination>(Destination.Folders) }

    fun navigate(destination: Destination) {
        backStack.add(destination)
    }

    fun pop() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    BackHandler(enabled = backStack.size > 1) { pop() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(dev.wndavenz.notely.ui.theme.NotelyColors.Background),
    ) {
        when (val destination = backStack.last()) {
            Destination.Folders -> FoldersScreen(
                repository = repository,
                onOpenList = { view -> navigate(Destination.NoteList(view)) },
                onOpenSettings = { navigate(Destination.Settings) },
                onOpenEditor = { note ->
                    navigate(Destination.Editor(note.id, note.folderId, "Folder"))
                },
                onCompose = { folderId ->
                    navigate(Destination.Editor(null, folderId, "Folder"))
                },
            )

            is Destination.NoteList -> NotesListScreen(
                repository = repository,
                view = destination.view,
                onBack = { pop() },
                onOpenEditor = { note: NoteEntity ->
                    navigate(
                        Destination.Editor(note.id, note.folderId, titleOf(destination.view)),
                    )
                },
                onCompose = { folderId ->
                    navigate(
                        Destination.Editor(null, folderId, titleOf(destination.view)),
                    )
                },
            )

            is Destination.Editor -> EditorScreen(
                repository = repository,
                appScope = appScope,
                noteId = destination.noteId,
                folderId = destination.folderId,
                parentTitle = destination.parentTitle,
                sessionKey = destination.sessionKey,
                onBack = { pop() },
            )

            Destination.Settings -> SettingsScreen(
                repository = repository,
                onBack = { pop() },
            )
        }
    }
}

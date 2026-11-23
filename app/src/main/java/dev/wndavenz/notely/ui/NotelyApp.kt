package dev.wndavenz.notely.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import dev.wndavenz.notely.data.NoteEntity
import dev.wndavenz.notely.data.NotesRepository
import dev.wndavenz.notely.ui.screens.EditorScreen
import dev.wndavenz.notely.ui.screens.FoldersScreen
import dev.wndavenz.notely.ui.screens.NotesListScreen
import dev.wndavenz.notely.ui.screens.SettingsScreen
import dev.wndavenz.notely.ui.theme.NotelyColors
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

/** Layar teratas beserta kedalamannya di tumpukan, untuk arah animasi. */
private data class NavTarget(val destination: Destination, val depth: Int)

private const val NAV_ANIM_MS = 320

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
            .background(NotelyColors.Background),
    ) {
        // Transisi geser gaya iOS: maju = layar baru masuk dari kanan dan
        // layar lama bergeser sepertiga ke kiri; mundur = kebalikannya.
        AnimatedContent(
            targetState = NavTarget(backStack.last(), backStack.lastIndex),
            transitionSpec = {
                val spec = tween<Float>(NAV_ANIM_MS, easing = FastOutSlowInEasing)
                val slideSpec = tween<IntOffset>(NAV_ANIM_MS, easing = FastOutSlowInEasing)
                if (targetState.depth > initialState.depth) {
                    (slideInHorizontally(slideSpec) { it } + fadeIn(spec)).togetherWith(
                        slideOutHorizontally(slideSpec) { -it / 3 } + fadeOut(spec),
                    )
                } else {
                    (slideInHorizontally(slideSpec) { -it / 3 } + fadeIn(spec)).togetherWith(
                        slideOutHorizontally(slideSpec) { it } + fadeOut(spec),
                    )
                }.using(SizeTransform(clip = false))
            },
            label = "navigasi",
        ) { target ->
            when (val destination = target.destination) {
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
}

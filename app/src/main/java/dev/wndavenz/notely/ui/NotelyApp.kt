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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.wndavenz.notely.data.NoteEntity
import dev.wndavenz.notely.data.NotesRepository
import dev.wndavenz.notely.ui.components.UpdateDialog
import dev.wndavenz.notely.ui.components.UpdateUiState
import dev.wndavenz.notely.ui.screens.EditorScreen
import dev.wndavenz.notely.ui.screens.FoldersScreen
import dev.wndavenz.notely.ui.screens.NotesListScreen
import dev.wndavenz.notely.ui.screens.SettingsScreen
import dev.wndavenz.notely.ui.theme.NotelyColors
import dev.wndavenz.notely.update.AppUpdater
import dev.wndavenz.notely.update.CheckResult
import dev.wndavenz.notely.update.UpdateInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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

    // --- Pembaruan mandiri (GitHub Releases) ---
    val context = LocalContext.current
    var updateState by remember { mutableStateOf<UpdateUiState>(UpdateUiState.Idle) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }

    fun runUpdateCheck(manual: Boolean) {
        if (updateState is UpdateUiState.Checking ||
            updateState is UpdateUiState.Downloading
        ) {
            return
        }
        if (manual) updateState = UpdateUiState.Checking
        appScope.launch {
            val result = AppUpdater.checkLatest(AppUpdater.currentVersion(context))
            val stillChecking = updateState is UpdateUiState.Checking
            when (result) {
                is CheckResult.Available ->
                    updateState = UpdateUiState.Available(result.info)

                CheckResult.UpToDate ->
                    if (manual && stillChecking) updateState = UpdateUiState.UpToDate

                CheckResult.Failed ->
                    if (manual && stillChecking) {
                        updateState = UpdateUiState.Failed(
                            "Tidak bisa memeriksa pembaruan. Periksa koneksi internet lalu coba lagi.",
                        )
                    } else if (stillChecking) {
                        updateState = UpdateUiState.Idle
                    }
            }
        }
    }

    fun startUpdateDownload(info: UpdateInfo) {
        if (!AppUpdater.canInstall(context)) {
            // Belum diizinkan memasang dari sumber tak dikenal: antar ke
            // pengaturannya dulu; dialog tetap tampil supaya bisa dicoba lagi.
            context.startActivity(AppUpdater.unknownSourcesIntent(context))
            return
        }
        updateState = UpdateUiState.Downloading(info, null)
        downloadJob = appScope.launch {
            try {
                val file = AppUpdater.download(context, info) { progress ->
                    updateState = UpdateUiState.Downloading(info, progress)
                }
                updateState = UpdateUiState.Idle
                context.startActivity(AppUpdater.installIntent(context, file))
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                updateState = UpdateUiState.Failed(
                    "Unduhan gagal. Periksa koneksi internet lalu coba lagi.",
                )
            }
        }
    }

    // Pemeriksaan otomatis tiap aplikasi dibuka / kembali ke depan.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) runUpdateCheck(manual = false)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
                    onCheckUpdate = { runUpdateCheck(manual = true) },
                )
            }
        }

        UpdateDialog(
            state = updateState,
            onDismiss = { updateState = UpdateUiState.Idle },
            onUpdate = { info -> startUpdateDownload(info) },
            onCancelDownload = {
                downloadJob?.cancel()
                updateState = UpdateUiState.Idle
            },
        )
    }
}

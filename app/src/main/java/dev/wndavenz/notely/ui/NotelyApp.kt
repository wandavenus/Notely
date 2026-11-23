package dev.wndavenz.notely.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import dev.wndavenz.notely.data.NoteEntity
import dev.wndavenz.notely.data.NotesRepository
import dev.wndavenz.notely.ui.screens.EditorScreen
import dev.wndavenz.notely.ui.screens.FoldersScreen
import dev.wndavenz.notely.ui.screens.NotesListScreen
import dev.wndavenz.notely.ui.screens.SettingsScreen
import dev.wndavenz.notely.ui.theme.NotelyColors
import dev.wndavenz.notely.ui.theme.NotelyText
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

/** Judul besar sebuah tujuan, bila ada (sumber animasi morph judul). */
private fun largeTitleOf(destination: Destination): String? = when (destination) {
    Destination.Folders -> "Folder"
    is Destination.NoteList -> titleOf(destination.view)
    Destination.Settings -> "Pengaturan"
    is Destination.Editor -> null
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

    val currentTarget = NavTarget(backStack.last(), backStack.lastIndex)

    // Morph judul gaya iOS: judul besar layar asal berjalan ke bilah atas
    // layar tujuan (mengecil, naik, berubah warna menjadi label kembali),
    // dan sebaliknya saat kembali. Kemajuan animasinya satu sumber di sini;
    // judul asli di kedua layar disembunyikan selama morph berjalan.
    var morphText by remember { mutableStateOf<String?>(null) }
    var morphForward by remember { mutableStateOf(true) }
    var morphFrom by remember { mutableStateOf<Destination?>(null) }
    var morphTo by remember { mutableStateOf<Destination?>(null) }
    val morphProgress = remember { Animatable(1f) }
    var previousTarget by remember { mutableStateOf<NavTarget?>(null) }

    LaunchedEffect(currentTarget) {
        val prev = previousTarget
        previousTarget = currentTarget
        if (prev != null && prev.destination != currentTarget.destination) {
            val forward = currentTarget.depth > prev.depth
            val text = largeTitleOf(if (forward) prev.destination else currentTarget.destination)
            if (text != null) {
                morphText = text
                morphForward = forward
                morphFrom = prev.destination
                morphTo = currentTarget.destination
                morphProgress.snapTo(0f)
                morphProgress.animateTo(1f, tween(NAV_ANIM_MS, easing = FastOutSlowInEasing))
            } else {
                morphText = null
            }
        }
    }
    val morphActive = morphText != null && morphProgress.value < 1f

    fun hideLargeTitle(destination: Destination): Boolean =
        morphActive && ((morphForward && destination == morphFrom) ||
            (!morphForward && destination == morphTo))

    fun hideBackLabel(destination: Destination): Boolean =
        morphActive && ((morphForward && destination == morphTo) ||
            (!morphForward && destination == morphFrom))

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(NotelyColors.Background),
    ) {
        val screenWidth = maxWidth

        // Transisi geser gaya iOS: maju = layar baru masuk dari kanan dan
        // layar lama bergeser sepertiga ke kiri; mundur = kebalikannya.
        AnimatedContent(
            targetState = currentTarget,
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
                    hideLargeTitle = hideLargeTitle(destination),
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
                    hideLargeTitle = hideLargeTitle(destination),
                    hideBackLabel = hideBackLabel(destination),
                )

                is Destination.Editor -> EditorScreen(
                    repository = repository,
                    appScope = appScope,
                    noteId = destination.noteId,
                    folderId = destination.folderId,
                    parentTitle = destination.parentTitle,
                    sessionKey = destination.sessionKey,
                    onBack = { pop() },
                    hideBackLabel = hideBackLabel(destination),
                )

                Destination.Settings -> SettingsScreen(
                    repository = repository,
                    onBack = { pop() },
                    hideBackLabel = hideBackLabel(destination),
                )
            }
        }

        // Lapisan morph judul: satu teks yang berjalan di atas kedua layar.
        val text = morphText
        if (text != null && morphProgress.value < 1f) {
            val p = morphProgress.value
            val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            // Ujung besar: x 20 (16 halaman + 4 inden), tepat di bawah bilah
            // 44 dp. Ujung kecil: label kembali pada bilah, x 32 (8 bilah +
            // 24 chevron), terpusat vertikal (atas = 11 dp dari bilah).
            val startX: androidx.compose.ui.unit.Dp
            val endX: androidx.compose.ui.unit.Dp
            val startY: androidx.compose.ui.unit.Dp
            val endY: androidx.compose.ui.unit.Dp
            if (morphForward) {
                startX = 20.dp - screenWidth / 3f * p
                endX = 32.dp + screenWidth * (1f - p)
                startY = statusTop + 44.dp
                endY = statusTop + 11.dp
            } else {
                startX = 32.dp + screenWidth * p
                endX = 20.dp - screenWidth / 3f * (1f - p)
                startY = statusTop + 11.dp
                endY = statusTop + 44.dp
            }
            val fontSize = if (morphForward) lerp(34.sp, 17.sp, p) else lerp(17.sp, 34.sp, p)
            val lineHeight = if (morphForward) lerp(41.sp, 22.sp, p) else lerp(22.sp, 41.sp, p)
            val color = if (morphForward) {
                lerp(NotelyColors.Label, NotelyColors.Blue, p)
            } else {
                lerp(NotelyColors.Blue, NotelyColors.Label, p)
            }
            val weight = if (morphForward) {
                if (p < 0.5f) FontWeight.Bold else FontWeight.Normal
            } else {
                if (p < 0.5f) FontWeight.Normal else FontWeight.Bold
            }
            BasicText(
                text = text,
                style = NotelyText.LargeTitle.copy(
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    fontWeight = weight,
                    color = color,
                ),
                modifier = Modifier.offset(
                    x = lerp(startX, endX, p),
                    y = lerp(startY, endY, p),
                ),
            )
        }
    }
}

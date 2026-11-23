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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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

    // Morph judul gaya iOS: judul besar layar asal berjalan ke bilah atas
    // layar tujuan (mengecil, naik, berubah warna menjadi label kembali),
    // dan sebaliknya saat kembali. Morph dipicu langsung dari navigate/pop
    // (bukan efek samping) supaya mulai di frame yang sama dengan geseran
    // layar — telat satu frame saja, judul asli sempat terlihat penuh dan
    // animasinya terasa berkedip.
    var morphText by remember { mutableStateOf<String?>(null) }
    var morphForward by remember { mutableStateOf(true) }
    var morphFrom by remember { mutableStateOf<Destination?>(null) }
    var morphTo by remember { mutableStateOf<Destination?>(null) }
    val morphProgress = remember { Animatable(1f) }
    val morphScope = rememberCoroutineScope()

    fun startMorph(from: Destination, to: Destination, forward: Boolean) {
        val text = largeTitleOf(if (forward) from else to)
        if (text == null) {
            morphText = null
            return
        }
        morphText = text
        morphForward = forward
        morphFrom = from
        morphTo = to
        morphScope.launch {
            morphProgress.snapTo(0f)
            morphProgress.animateTo(1f, tween(NAV_ANIM_MS, easing = FastOutSlowInEasing))
        }
    }

    fun navigate(destination: Destination) {
        val from = backStack.last()
        backStack.add(destination)
        startMorph(from, destination, forward = true)
    }

    fun pop() {
        if (backStack.size > 1) {
            val from = backStack.last()
            backStack.removeAt(backStack.lastIndex)
            startMorph(from, backStack.last(), forward = false)
        }
    }

    BackHandler(enabled = backStack.size > 1) { pop() }

    val currentTarget = NavTarget(backStack.last(), backStack.lastIndex)
    val morphActive = morphText != null && morphProgress.value < 1f

    // Di 15% terakhir animasi, teks morph memudar dan judul asli di layar
    // yang DATANG memudar masuk, supaya titik mendaratnya mulus. Judul di
    // layar yang PERGI tetap tersembunyi selama morph (teksnya dibawa
    // oleh lapisan morph).
    val settle = if (morphActive) {
        ((morphProgress.value - 0.85f) / 0.15f).coerceIn(0f, 1f)
    } else {
        1f
    }

    fun largeTitleAlpha(destination: Destination): Float {
        val owner = if (morphForward) morphFrom else morphTo
        if (!morphActive || destination != owner) return 1f
        // Maju: pemilik judul besar adalah layar yang pergi -> tetap 0.
        // Mundur: pemiliknya layar yang datang -> pudar masuk di akhir.
        return if (morphForward) 0f else settle
    }

    fun backLabelAlpha(destination: Destination): Float {
        val owner = if (morphForward) morphTo else morphFrom
        if (!morphActive || destination != owner) return 1f
        // Maju: pemilik label adalah layar yang datang -> pudar masuk.
        // Mundur: pemiliknya layar yang pergi -> tetap 0.
        return if (morphForward) settle else 0f
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(NotelyColors.Background),
    ) {
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
                    largeTitleAlpha = largeTitleAlpha(destination),
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
                    largeTitleAlpha = largeTitleAlpha(destination),
                    backLabelAlpha = backLabelAlpha(destination),
                )

                is Destination.Editor -> EditorScreen(
                    repository = repository,
                    appScope = appScope,
                    noteId = destination.noteId,
                    folderId = destination.folderId,
                    parentTitle = destination.parentTitle,
                    sessionKey = destination.sessionKey,
                    onBack = { pop() },
                    backLabelAlpha = backLabelAlpha(destination),
                )

                Destination.Settings -> SettingsScreen(
                    repository = repository,
                    onBack = { pop() },
                    backLabelAlpha = backLabelAlpha(destination),
                )
            }
        }

        // Lapisan morph judul: satu teks yang berjalan di atas kedua layar.
        // Jalurnya vertikal murni di antara dua posisi diam judul (besar:
        // x 20, tepat di bawah bilah 44 dp; kecil: label kembali pada
        // bilah, x 32 = 8 bilah + 24 chevron, atas 11 dp dari bilah) —
        // tidak mengikuti geseran horizontal layar, supaya tidak melengkung.
        val text = morphText
        if (text != null && morphProgress.value < 1f) {
            val p = morphProgress.value
            val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val startX = if (morphForward) 20.dp else 32.dp
            val endX = if (morphForward) 32.dp else 20.dp
            val startY = statusTop + if (morphForward) 44.dp else 11.dp
            val endY = statusTop + if (morphForward) 11.dp else 44.dp
            val fontSize = if (morphForward) lerp(34.sp, 17.sp, p) else lerp(17.sp, 34.sp, p)
            val lineHeight = if (morphForward) lerp(41.sp, 22.sp, p) else lerp(22.sp, 41.sp, p)
            val color = if (morphForward) {
                lerp(NotelyColors.Label, NotelyColors.Blue, p)
            } else {
                lerp(NotelyColors.Blue, NotelyColors.Label, p)
            }
            // Ketebalan berubah di zona pudar akhir, supaya tidak terasa patah.
            val weight = if (morphForward) {
                if (p < 0.85f) FontWeight.Bold else FontWeight.Normal
            } else {
                if (p < 0.85f) FontWeight.Normal else FontWeight.Bold
            }
            BasicText(
                text = text,
                style = NotelyText.LargeTitle.copy(
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    fontWeight = weight,
                    color = color,
                ),
                modifier = Modifier
                    .alpha(1f - settle)
                    .offset(
                        x = lerp(startX, endX, p),
                        y = lerp(startY, endY, p),
                    ),
            )
        }
    }
}

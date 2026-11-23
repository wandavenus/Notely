package dev.wndavenz.notely.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.SquarePen
import dev.wndavenz.notely.data.NoteEntity
import dev.wndavenz.notely.data.NotesRepository
import dev.wndavenz.notely.ui.ListView
import dev.wndavenz.notely.ui.NotesListViewModel
import dev.wndavenz.notely.ui.components.EmptyStateText
import dev.wndavenz.notely.ui.components.IosActionSheet
import dev.wndavenz.notely.ui.components.IosBottomBar
import dev.wndavenz.notely.ui.components.IosConfirmDialog
import dev.wndavenz.notely.ui.components.IosIconButton
import dev.wndavenz.notely.ui.components.IosSearchField
import dev.wndavenz.notely.ui.components.IosTextButton
import dev.wndavenz.notely.ui.components.IosTopBar
import dev.wndavenz.notely.ui.components.NoteCard
import dev.wndavenz.notely.ui.components.NoteSectionHeader
import dev.wndavenz.notely.ui.components.SheetAction
import dev.wndavenz.notely.ui.theme.NotelyColors
import dev.wndavenz.notely.ui.theme.NotelyText
import dev.wndavenz.notely.ui.titleOf
import dev.wndavenz.notely.ui.viewModelFactory
import dev.wndavenz.notely.utils.NoteDateGroup
import dev.wndavenz.notely.utils.dateGroupOf

/** Daftar catatan di dalam folder, tampilan Tersimpan, atau Baru Dihapus. */
@Composable
fun NotesListScreen(
    repository: NotesRepository,
    view: ListView,
    onBack: () -> Unit,
    onOpenEditor: (NoteEntity) -> Unit,
    onCompose: (Long) -> Unit,
    hideLargeTitle: Boolean = false,
    hideBackLabel: Boolean = false,
) {
    val viewModel: NotesListViewModel = viewModel(
        key = "list-$view",
        factory = viewModelFactory { NotesListViewModel(repository, view) },
    )
    val notes by viewModel.notes.collectAsStateWithLifecycle()

    val searchState = rememberTextFieldState()
    val query = searchState.text.toString()
    val visible = remember(notes, query) {
        if (query.isBlank()) {
            notes
        } else {
            notes.filter { it.body.contains(query, ignoreCase = true) }
        }
    }

    var actionNote by remember { mutableStateOf<NoteEntity?>(null) }
    var confirmEmptyTrash by remember { mutableStateOf(false) }

    val isTrash = view is ListView.Trash
    val composeFolderId = (view as? ListView.Folder)?.id ?: 1L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        IosTopBar(
            backLabel = "Folder",
            onBack = onBack,
            backLabelAlpha = if (hideBackLabel) 0f else 1f,
            trailing = {
                if (isTrash && notes.isNotEmpty()) {
                    IosTextButton(
                        text = "Kosongkan",
                        onClick = { confirmEmptyTrash = true },
                        color = NotelyColors.Destructive,
                    )
                }
            },
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
        ) {
            item {
                BasicText(
                    text = titleOf(view),
                    style = NotelyText.LargeTitle,
                    modifier = Modifier
                        .alpha(if (hideLargeTitle) 0f else 1f)
                        .padding(start = 4.dp, bottom = 10.dp),
                )
                IosSearchField(state = searchState, placeholder = "Cari")
                Spacer(Modifier.height(6.dp))
            }

            if (visible.isEmpty()) {
                item {
                    EmptyStateText(
                        if (isTrash) {
                            "Tidak ada catatan yang dihapus."
                        } else {
                            "Tidak ada catatan."
                        },
                    )
                }
            } else if (query.isNotBlank()) {
                items(visible, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        onClick = { onOpenEditor(note) },
                        onLongClick = { actionNote = note },
                    )
                    Spacer(Modifier.height(10.dp))
                }
            } else {
                val pinned = visible.filter { it.pinned }
                val rest = visible.filter { !it.pinned }
                if (pinned.isNotEmpty() && !isTrash) {
                    item { NoteSectionHeader(title = "Disematkan", pinned = true) }
                    items(pinned, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onClick = { onOpenEditor(note) },
                            onLongClick = { actionNote = note },
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
                val grouped = rest.groupBy { dateGroupOf(it.updatedAt) }
                for (group in NoteDateGroup.entries) {
                    val groupNotes = grouped[group] ?: continue
                    item { NoteSectionHeader(title = group.label, pinned = false) }
                    items(groupNotes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onClick = { onOpenEditor(note) },
                            onLongClick = { actionNote = note },
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }

        if (isTrash) {
            IosBottomBar(centerText = "${notes.size} Catatan")
        } else {
            IosBottomBar(
                centerText = "${notes.size} Catatan",
                trailing = {
                    IosIconButton(
                        icon = Lucide.SquarePen,
                        contentDescription = "Catatan baru",
                        onClick = { onCompose(composeFolderId) },
                    )
                },
            )
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }

    actionNote?.let { note ->
        if (isTrash) {
            IosActionSheet(
                actions = listOf(
                    SheetAction(label = "Pulihkan") {
                        viewModel.restore(note)
                        actionNote = null
                    },
                    SheetAction(label = "Hapus Permanen", color = NotelyColors.Destructive) {
                        viewModel.deleteForever(note)
                        actionNote = null
                    },
                ),
                onDismiss = { actionNote = null },
            )
        } else {
            IosActionSheet(
                actions = listOf(
                    SheetAction(label = if (note.pinned) "Lepas Sematan" else "Sematkan") {
                        viewModel.setPinned(note, !note.pinned)
                        actionNote = null
                    },
                    SheetAction(label = "Hapus", color = NotelyColors.Destructive) {
                        viewModel.moveToTrash(note)
                        actionNote = null
                    },
                ),
                onDismiss = { actionNote = null },
            )
        }
    }

    if (confirmEmptyTrash) {
        IosConfirmDialog(
            title = "Kosongkan Baru Dihapus?",
            message = "Semua catatan yang dihapus akan hilang selamanya.",
            confirmLabel = "Kosongkan",
            onConfirm = {
                viewModel.emptyTrash()
                confirmEmptyTrash = false
            },
            onDismiss = { confirmEmptyTrash = false },
        )
    }
}

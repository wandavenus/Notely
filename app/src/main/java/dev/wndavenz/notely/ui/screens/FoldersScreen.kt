package dev.wndavenz.notely.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.CircleMinus
import com.composables.icons.lucide.Folder
import com.composables.icons.lucide.FolderPlus
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pin
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.SquarePen
import com.composables.icons.lucide.Trash2
import dev.wndavenz.notely.data.FolderSummary
import dev.wndavenz.notely.data.NoteEntity
import dev.wndavenz.notely.data.NotesRepository
import dev.wndavenz.notely.ui.FoldersViewModel
import dev.wndavenz.notely.ui.ListView
import dev.wndavenz.notely.ui.components.EmptyStateText
import dev.wndavenz.notely.ui.components.IosBottomBar
import dev.wndavenz.notely.ui.components.IosConfirmDialog
import dev.wndavenz.notely.ui.components.IosDivider
import dev.wndavenz.notely.ui.components.IosGroupCard
import dev.wndavenz.notely.ui.components.IosIconButton
import dev.wndavenz.notely.ui.components.IosSearchField
import dev.wndavenz.notely.ui.components.IosTextButton
import dev.wndavenz.notely.ui.components.IosTextInputDialog
import dev.wndavenz.notely.ui.components.NoteCard
import dev.wndavenz.notely.ui.theme.NotelyColors
import dev.wndavenz.notely.ui.theme.NotelyText
import dev.wndavenz.notely.ui.viewModelFactory

/** Layar depan: daftar folder, tampilan pintar, dan pencarian global. */
@Composable
fun FoldersScreen(
    repository: NotesRepository,
    onOpenList: (ListView) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenEditor: (NoteEntity) -> Unit,
    onCompose: (Long) -> Unit,
) {
    val viewModel: FoldersViewModel = viewModel(
        key = "folders",
        factory = viewModelFactory { FoldersViewModel(repository) },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val searchState = rememberTextFieldState()
    LaunchedEffect(searchState) {
        snapshotFlow { searchState.text.toString() }.collect { viewModel.onQueryChange(it) }
    }
    val searching = searchState.text.isNotEmpty()

    var editing by remember { mutableStateOf(false) }
    var showCreateFolder by remember { mutableStateOf(false) }
    var folderToDelete by remember { mutableStateOf<FolderSummary?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IosTextButton(
                text = if (editing) "Selesai" else "Edit",
                onClick = { editing = !editing },
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
        ) {
            item {
                BasicText(
                    text = "Folder",
                    style = NotelyText.LargeTitle,
                    modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
                )
                IosSearchField(state = searchState, placeholder = "Telusuri")
                Spacer(Modifier.height(14.dp))
            }

            if (searching) {
                if (state.searchResults.isEmpty()) {
                    item { EmptyStateText("Tidak ada hasil untuk pencarian ini.") }
                } else {
                    items(state.searchResults, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onClick = { onOpenEditor(note) },
                            onLongClick = {},
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            } else {
                item {
                    IosGroupCard {
                        state.folders.forEachIndexed { index, summary ->
                            if (index > 0) IosDivider(insetStart = 48.dp)
                            FolderRow(
                                summary = summary,
                                editing = editing && index > 0,
                                onDelete = { folderToDelete = summary },
                                onClick = {
                                    onOpenList(ListView.Folder(summary.id, summary.name))
                                },
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(18.dp)) }
                item {
                    IosGroupCard {
                        SmartRow(
                            icon = Lucide.Pin,
                            label = "Tersimpan",
                            count = state.pinnedCount,
                            onClick = { onOpenList(ListView.Pinned) },
                        )
                        IosDivider(insetStart = 48.dp)
                        SmartRow(
                            icon = Lucide.Trash2,
                            label = "Baru Dihapus",
                            count = state.trashCount,
                            onClick = { onOpenList(ListView.Trash) },
                        )
                        IosDivider(insetStart = 48.dp)
                        SmartRow(
                            icon = Lucide.Settings,
                            label = "Pengaturan",
                            count = null,
                            onClick = onOpenSettings,
                        )
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }

        IosBottomBar(
            leading = {
                IosIconButton(
                    icon = Lucide.FolderPlus,
                    contentDescription = "Folder baru",
                    onClick = { showCreateFolder = true },
                )
            },
            trailing = {
                IosIconButton(
                    icon = Lucide.SquarePen,
                    contentDescription = "Catatan baru",
                    onClick = { onCompose(state.defaultFolderId) },
                )
            },
        )
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }

    if (showCreateFolder) {
        IosTextInputDialog(
            title = "Folder Baru",
            placeholder = "Nama folder",
            confirmLabel = "Buat",
            onConfirm = { name ->
                viewModel.createFolder(name)
                showCreateFolder = false
            },
            onDismiss = { showCreateFolder = false },
        )
    }

    folderToDelete?.let { summary ->
        IosConfirmDialog(
            title = "Hapus folder \"${summary.name}\"?",
            message = "Catatan di dalamnya dipindahkan ke folder Catatan.",
            confirmLabel = "Hapus",
            onConfirm = {
                viewModel.deleteFolder(summary.id)
                folderToDelete = null
            },
            onDismiss = { folderToDelete = null },
        )
    }
}

@Composable
private fun FolderRow(
    summary: FolderSummary,
    editing: Boolean,
    onDelete: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (editing) {
            Image(
                imageVector = Lucide.CircleMinus,
                contentDescription = "Hapus folder",
                colorFilter = ColorFilter.tint(NotelyColors.Destructive),
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = onDelete),
            )
            Spacer(Modifier.width(8.dp))
        }
        Image(
            imageVector = Lucide.Folder,
            contentDescription = null,
            colorFilter = ColorFilter.tint(NotelyColors.Blue),
            modifier = Modifier.size(21.dp),
        )
        Spacer(Modifier.width(12.dp))
        BasicText(
            text = summary.name,
            style = NotelyText.Body,
            modifier = Modifier.weight(1f),
        )
        BasicText(
            text = summary.noteCount.toString(),
            style = NotelyText.Callout.copy(color = NotelyColors.SecondaryLabel),
        )
        Spacer(Modifier.width(4.dp))
        Image(
            imageVector = Lucide.ChevronRight,
            contentDescription = null,
            colorFilter = ColorFilter.tint(NotelyColors.TertiaryLabel),
            modifier = Modifier.size(15.dp),
        )
    }
}

@Composable
private fun SmartRow(
    icon: ImageVector,
    label: String,
    count: Int?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            imageVector = icon,
            contentDescription = null,
            colorFilter = ColorFilter.tint(NotelyColors.Blue),
            modifier = Modifier.size(21.dp),
        )
        Spacer(Modifier.width(12.dp))
        BasicText(text = label, style = NotelyText.Body, modifier = Modifier.weight(1f))
        if (count != null) {
            BasicText(
                text = count.toString(),
                style = NotelyText.Callout.copy(color = NotelyColors.SecondaryLabel),
            )
            Spacer(Modifier.width(4.dp))
        }
        Image(
            imageVector = Lucide.ChevronRight,
            contentDescription = null,
            colorFilter = ColorFilter.tint(NotelyColors.TertiaryLabel),
            modifier = Modifier.size(15.dp),
        )
    }
}

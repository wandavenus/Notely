package dev.wndavenz.notely.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import dev.wndavenz.notely.data.NotesRepository
import dev.wndavenz.notely.update.AppUpdater
import dev.wndavenz.notely.ui.SettingsViewModel
import dev.wndavenz.notely.ui.components.IosConfirmDialog
import dev.wndavenz.notely.ui.components.IosDivider
import dev.wndavenz.notely.ui.components.IosGroupCard
import dev.wndavenz.notely.ui.components.IosSwitch
import dev.wndavenz.notely.ui.components.IosTopBar
import dev.wndavenz.notely.ui.theme.NotelyColors
import dev.wndavenz.notely.ui.theme.NotelyText
import dev.wndavenz.notely.ui.viewModelFactory

/** Layar Pengaturan: ringkasan data, pengosongan sampah, dan info aplikasi. */
@Composable
fun SettingsScreen(
    repository: NotesRepository,
    onBack: () -> Unit,
    onCheckUpdate: () -> Unit = {},
) {
    val viewModel: SettingsViewModel = viewModel(
        key = "settings",
        factory = viewModelFactory { SettingsViewModel(repository) },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmEmptyTrash by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val installedVersion = remember(context) { AppUpdater.currentVersion(context) }
    var autoCheck by remember { mutableStateOf(AppUpdater.autoCheckEnabled(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        IosTopBar(backLabel = "Folder", onBack = onBack)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
        ) {
            item {
                BasicText(
                    text = "Pengaturan",
                    style = NotelyText.LargeTitle,
                    modifier = Modifier.padding(start = 4.dp, bottom = 14.dp),
                )
            }
            item {
                IosGroupCard {
                    InfoRow(label = "Jumlah Catatan", value = state.noteCount.toString())
                    IosDivider(insetStart = 14.dp)
                    InfoRow(label = "Folder", value = state.folderCount.toString())
                    IosDivider(insetStart = 14.dp)
                    InfoRow(label = "Disematkan", value = state.pinnedCount.toString())
                    IosDivider(insetStart = 14.dp)
                    InfoRow(label = "Baru Dihapus", value = state.trashCount.toString())
                }
            }
            item { Spacer(Modifier.height(18.dp)) }
            item {
                IosGroupCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = state.trashCount > 0) {
                                confirmEmptyTrash = true
                            }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BasicText(
                            text = "Kosongkan Baru Dihapus",
                            style = NotelyText.Body.copy(
                                color = if (state.trashCount > 0) {
                                    NotelyColors.Destructive
                                } else {
                                    NotelyColors.TertiaryLabel
                                },
                            ),
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(18.dp)) }
            item {
                IosGroupCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onCheckUpdate)
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BasicText(
                            text = "Periksa Pembaruan",
                            style = NotelyText.Body,
                            modifier = Modifier.weight(1f),
                        )
                        Image(
                            imageVector = Lucide.ChevronRight,
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(NotelyColors.SecondaryLabel),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IosDivider(insetStart = 14.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BasicText(
                            text = "Periksa Pembaruan Otomatis",
                            style = NotelyText.Body,
                            modifier = Modifier.weight(1f),
                        )
                        IosSwitch(
                            checked = autoCheck,
                            onCheckedChange = { enabled ->
                                autoCheck = enabled
                                AppUpdater.setAutoCheckEnabled(context, enabled)
                            },
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(18.dp)) }
            item {
                IosGroupCard {
                    InfoRow(label = "Notely", value = null)
                    IosDivider(insetStart = 14.dp)
                    InfoRow(label = "Versi", value = installedVersion)
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
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

@Composable
private fun InfoRow(label: String, value: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = label,
            style = NotelyText.Body,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            BasicText(
                text = value,
                style = NotelyText.Callout.copy(color = NotelyColors.SecondaryLabel),
            )
        }
    }
}

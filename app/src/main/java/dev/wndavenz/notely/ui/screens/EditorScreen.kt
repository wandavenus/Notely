package dev.wndavenz.notely.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.Ellipsis
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pin
import dev.wndavenz.notely.data.NotesRepository
import dev.wndavenz.notely.ui.EditorViewModel
import dev.wndavenz.notely.ui.components.IosActionSheet
import dev.wndavenz.notely.ui.components.IosConfirmDialog
import dev.wndavenz.notely.ui.components.IosIconButton
import dev.wndavenz.notely.ui.components.IosTopBar
import dev.wndavenz.notely.ui.components.SheetAction
import dev.wndavenz.notely.ui.theme.NotelyColors
import dev.wndavenz.notely.ui.theme.NotelyText
import dev.wndavenz.notely.ui.viewModelFactory
import dev.wndavenz.notely.utils.DateFormat
import kotlinx.coroutines.CoroutineScope

/** Mewarnai baris pertama editor sebagai judul (tebal, lebih besar), gaya Apple Notes. */
private object TitleOutputTransformation : OutputTransformation {
    override fun TextFieldBuffer.transformOutput() {
        val text = asCharSequence()
        if (text.isEmpty()) return
        val newline = text.indexOf('\n')
        val end = if (newline == -1) text.length else newline
        if (end > 0) {
            addStyle(
                SpanStyle(fontWeight = FontWeight.Bold, fontSize = 21.sp),
                start = 0,
                end = end,
            )
        }
    }
}

/** Editor catatan: satu kolom teks, simpan otomatis, judul dari baris pertama. */
@Composable
fun EditorScreen(
    repository: NotesRepository,
    appScope: CoroutineScope,
    noteId: Long?,
    folderId: Long,
    parentTitle: String,
    sessionKey: Long,
    onBack: () -> Unit,
) {
    val viewModel: EditorViewModel = viewModel(
        key = "editor-$sessionKey",
        factory = viewModelFactory {
            EditorViewModel(repository, appScope, noteId, folderId)
        },
    )
    val ui by viewModel.state.collectAsStateWithLifecycle()

    val textState = rememberTextFieldState()
    var initialized by remember { mutableStateOf(false) }
    LaunchedEffect(ui.loaded) {
        if (ui.loaded && !initialized) {
            textState.setTextAndPlaceCursorAtEnd(ui.initialBody)
            initialized = true
        }
    }
    LaunchedEffect(textState, initialized) {
        if (initialized) {
            snapshotFlow { textState.text.toString() }.collect { viewModel.onBodyChanged(it) }
        }
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.saveNow(textState.text.toString()) }
    }

    var showSheet by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            // Area bawah naik mengikuti tinggi keyboard saat mengedit,
            // supaya bilah bawah dan teks tidak tertutup keyboard.
            .imePadding(),
    ) {
        IosTopBar(
            backLabel = parentTitle,
            onBack = onBack,
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IosIconButton(
                        icon = Lucide.Pin,
                        contentDescription = if (ui.pinned) "Lepas sematan" else "Sematkan",
                        onClick = { viewModel.togglePin() },
                        tint = if (ui.pinned) NotelyColors.Blue else NotelyColors.TertiaryLabel,
                    )
                    IosIconButton(
                        icon = Lucide.Ellipsis,
                        contentDescription = "Opsi catatan",
                        onClick = { showSheet = true },
                    )
                }
            },
        )
        BasicText(
            text = DateFormat.fullDateTime(ui.updatedAt),
            style = NotelyText.Caption.copy(textAlign = TextAlign.Center),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        )
        BasicTextField(
            state = textState,
            modifier = Modifier
                .fillMaxSize()
                // Ruang bawah besar: baris terakhir + karet tidak mepet
                // tepi bawah layar / tepi atas keyboard.
                .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 48.dp),
            textStyle = NotelyText.EditorBody,
            cursorBrush = SolidColor(NotelyColors.Blue),
            outputTransformation = TitleOutputTransformation,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                autoCorrectEnabled = true,
                keyboardType = KeyboardType.Text,
            ),
            lineLimits = TextFieldLineLimits.MultiLine(),
            decorator = { innerTextField ->
                Box {
                    if (textState.text.isEmpty()) {
                        BasicText(
                            text = "Catatan Baru",
                            style = NotelyText.EditorBody.copy(
                                color = NotelyColors.TertiaryLabel,
                            ),
                        )
                    }
                    innerTextField()
                }
            },
        )
    }

    if (showSheet) {
        IosActionSheet(
            actions = listOf(
                SheetAction(label = if (ui.pinned) "Lepas Sematan" else "Sematkan") {
                    viewModel.togglePin()
                    showSheet = false
                },
                SheetAction(label = "Hapus", color = NotelyColors.Destructive) {
                    showSheet = false
                    confirmDelete = true
                },
            ),
            onDismiss = { showSheet = false },
        )
    }

    if (confirmDelete) {
        IosConfirmDialog(
            title = "Hapus catatan ini?",
            confirmLabel = "Hapus",
            onConfirm = {
                viewModel.moveToTrash()
                confirmDelete = false
                onBack()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

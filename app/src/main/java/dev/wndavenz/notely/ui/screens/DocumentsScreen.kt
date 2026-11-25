package dev.wndavenz.notely.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.Lucide
import dev.wndavenz.notely.documents.DocumentAccess
import dev.wndavenz.notely.documents.DocumentFile
import dev.wndavenz.notely.documents.PinnedDocuments
import dev.wndavenz.notely.documents.formatFileSize
import dev.wndavenz.notely.ui.DocumentsUiState
import dev.wndavenz.notely.ui.DocumentsViewModel
import dev.wndavenz.notely.ui.components.EmptyStateText
import dev.wndavenz.notely.ui.components.IosActionSheet
import dev.wndavenz.notely.ui.components.IosBottomBar
import dev.wndavenz.notely.ui.components.IosConfirmDialog
import dev.wndavenz.notely.ui.components.IosSearchField
import dev.wndavenz.notely.ui.components.IosTopBar
import dev.wndavenz.notely.ui.components.NoteSectionHeader
import dev.wndavenz.notely.ui.components.SheetAction
import dev.wndavenz.notely.ui.theme.NotelyColors
import dev.wndavenz.notely.ui.theme.NotelyText
import dev.wndavenz.notely.ui.viewModelFactory
import dev.wndavenz.notely.utils.DateFormat
import dev.wndavenz.notely.utils.NoteDateGroup
import dev.wndavenz.notely.utils.dateGroupOf
import java.io.File

/**
 * Daftar dokumen .md / .txt hasil pindai otomatis seluruh penyimpanan.
 * Dokumen hanya dibaca, tidak disalin dan tidak diubah.
 */
@Composable
fun DocumentsScreen(
    onBack: () -> Unit,
    onOpenDocument: (DocumentFile) -> Unit,
) {
    val viewModel: DocumentsViewModel = viewModel(
        key = "documents",
        factory = viewModelFactory { DocumentsViewModel() },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var pinnedPaths by remember { mutableStateOf(PinnedDocuments.paths(context)) }
    var actionDocument by remember { mutableStateOf<DocumentFile?>(null) }
    var confirmDelete by remember { mutableStateOf<DocumentFile?>(null) }

    // Android 10: izin baca klasik diminta lewat dialog sistem.
    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        viewModel.refresh(DocumentAccess.hasAccess(context))
    }

    // Segarkan tiap layar kembali ke depan: izin bisa baru saja diberikan
    // dari Pengaturan, dan file bisa berubah selama Notely di latar.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                pinnedPaths = PinnedDocuments.paths(context)
                viewModel.refresh(DocumentAccess.hasAccess(context))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val searchState = rememberTextFieldState()
    val query = searchState.text.toString()
    val ready = state as? DocumentsUiState.Ready
    val visible = remember(ready, query) {
        val all = ready?.documents ?: emptyList()
        if (query.isBlank()) {
            all
        } else {
            all.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.relativePath.contains(query, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .imePadding(),
    ) {
        IosTopBar(backLabel = "Folder", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
        ) {
            item {
                BasicText(
                    text = "Dokumen",
                    style = NotelyText.LargeTitle,
                    modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
                )
                IosSearchField(state = searchState, placeholder = "Cari")
                Spacer(Modifier.height(14.dp))
            }

            when (val current = state) {
                DocumentsUiState.NeedAccess -> item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        BasicText(
                            text = "Notely perlu akses file untuk memindai dokumen .md dan .txt di penyimpananmu.",
                            style = NotelyText.Callout.copy(
                                color = NotelyColors.SecondaryLabel,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            ),
                        )
                        Spacer(Modifier.height(18.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(NotelyColors.Blue)
                                .clickable {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        context.startActivity(
                                            DocumentAccess.allFilesAccessIntent(context),
                                        )
                                    } else {
                                        legacyPermissionLauncher.launch(
                                            Manifest.permission.READ_EXTERNAL_STORAGE,
                                        )
                                    }
                                }
                                .padding(horizontal = 18.dp, vertical = 11.dp),
                        ) {
                            BasicText(
                                text = "Izinkan Akses File",
                                style = NotelyText.BodySemibold.copy(
                                    color = androidx.compose.ui.graphics.Color.White,
                                ),
                            )
                        }
                    }
                }

                DocumentsUiState.Scanning -> item {
                    EmptyStateText("Memindai penyimpanan…")
                }

                is DocumentsUiState.Ready -> {
                    if (visible.isEmpty()) {
                        item {
                            EmptyStateText(
                                if (query.isBlank()) {
                                    "Tidak ada file .md atau .txt ditemukan."
                                } else {
                                    "Tidak ada hasil untuk pencarian ini."
                                },
                            )
                        }
                    } else if (query.isNotBlank()) {
                        // Mode cari: daftar datar, sama seperti Catatan.
                        items(visible, key = { it.path }) { document ->
                            DocumentCard(
                                document = document,
                                onClick = { onOpenDocument(document) },
                                onLongClick = { actionDocument = document },
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                    } else {
                        // Seksi Disematkan + kelompok tanggal, sama seperti
                        // halaman Catatan.
                        val pinned = visible.filter { it.path in pinnedPaths }
                        val rest = visible.filter { it.path !in pinnedPaths }
                        if (pinned.isNotEmpty()) {
                            item { NoteSectionHeader(title = "Disematkan", pinned = true) }
                            items(pinned, key = { it.path }) { document ->
                                DocumentCard(
                                    document = document,
                                    onClick = { onOpenDocument(document) },
                                    onLongClick = { actionDocument = document },
                                )
                                Spacer(Modifier.height(10.dp))
                            }
                        }
                        val grouped = rest.groupBy { dateGroupOf(it.modifiedAt) }
                        for (group in NoteDateGroup.entries) {
                            val groupDocuments = grouped[group] ?: continue
                            item { NoteSectionHeader(title = group.label, pinned = false) }
                            items(groupDocuments, key = { it.path }) { document ->
                                DocumentCard(
                                    document = document,
                                    onClick = { onOpenDocument(document) },
                                    onLongClick = { actionDocument = document },
                                )
                                Spacer(Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }

        IosBottomBar(
            centerText = when (val current = state) {
                is DocumentsUiState.Ready -> "${current.documents.size} Dokumen"
                else -> null
            },
        )
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }

    // Menu tekan lama, sama seperti kartu catatan.
    actionDocument?.let { document ->
        val isPinned = document.path in pinnedPaths
        IosActionSheet(
            actions = listOf(
                SheetAction(label = if (isPinned) "Lepas Sematan" else "Sematkan") {
                    PinnedDocuments.setPinned(context, document.path, !isPinned)
                    pinnedPaths = PinnedDocuments.paths(context)
                    actionDocument = null
                },
                SheetAction(label = "Hapus", color = NotelyColors.Destructive) {
                    confirmDelete = document
                    actionDocument = null
                },
            ),
            onDismiss = { actionDocument = null },
        )
    }

    confirmDelete?.let { document ->
        IosConfirmDialog(
            title = "Hapus File?",
            message = "\"${document.name}\" akan dihapus permanen dari penyimpanan.",
            confirmLabel = "Hapus",
            onConfirm = {
                File(document.path).delete()
                PinnedDocuments.setPinned(context, document.path, false)
                pinnedPaths = PinnedDocuments.paths(context)
                confirmDelete = null
                viewModel.refresh(DocumentAccess.hasAccess(context))
            },
            onDismiss = { confirmDelete = null },
        )
    }
}

/** Kartu satu dokumen: nama berkas, jalur relatif + tanggal + ukuran.
 *  Ketuk = buka, tekan lama = menu (sematkan / hapus). */
@Composable
private fun DocumentCard(
    document: DocumentFile,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(NotelyColors.Card)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            imageVector = Lucide.FileText,
            contentDescription = null,
            colorFilter = ColorFilter.tint(NotelyColors.Blue),
            modifier = Modifier.size(21.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            BasicText(
                text = document.name,
                style = NotelyText.BodySemibold.copy(color = NotelyColors.Label),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            BasicText(
                text = "${document.relativePath}  ·  " +
                    "${DateFormat.time(document.modifiedAt)}  ·  " +
                    formatFileSize(document.sizeBytes),
                style = NotelyText.Footnote,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

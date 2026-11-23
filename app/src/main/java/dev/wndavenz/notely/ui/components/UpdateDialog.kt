package dev.wndavenz.notely.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.wndavenz.notely.ui.theme.NotelyColors
import dev.wndavenz.notely.ui.theme.NotelyText
import dev.wndavenz.notely.update.UpdateInfo

/** Keadaan dialog pembaruan aplikasi. */
sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Available(val info: UpdateInfo) : UpdateUiState
    data class Downloading(val info: UpdateInfo, val progress: Float?) : UpdateUiState
    data class Failed(val message: String) : UpdateUiState
}

/**
 * Dialog pembaruan gaya iOS: menawarkan versi baru, menampilkan kemajuan
 * unduhan, dan melaporkan hasil pemeriksaan manual.
 */
@Composable
fun UpdateDialog(
    state: UpdateUiState,
    onDismiss: () -> Unit,
    onUpdate: (UpdateInfo) -> Unit,
    onCancelDownload: () -> Unit,
) {
    if (state is UpdateUiState.Idle) return
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(270.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(NotelyColors.SheetBackground),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (state) {
                    is UpdateUiState.Checking -> {
                        DialogTitle("Memeriksa Pembaruan")
                        DialogMessage("Sebentar ya…")
                    }

                    is UpdateUiState.UpToDate -> {
                        DialogTitle("Sudah Terbaru")
                        DialogMessage("Notely kamu sudah versi paling baru.")
                    }

                    is UpdateUiState.Available -> {
                        DialogTitle("Pembaruan Tersedia")
                        DialogMessage("Versi ${state.info.version} siap dipasang.")
                    }

                    is UpdateUiState.Downloading -> {
                        DialogTitle("Mengunduh Pembaruan")
                        Spacer(Modifier.height(12.dp))
                        ProgressBar(progress = state.progress)
                        Spacer(Modifier.height(8.dp))
                        DialogMessage(
                            if (state.progress != null) {
                                "${(state.progress * 100).toInt()}%"
                            } else {
                                "Mengunduh…"
                            },
                        )
                    }

                    is UpdateUiState.Failed -> {
                        DialogTitle("Pembaruan Gagal")
                        DialogMessage(state.message)
                    }

                    is UpdateUiState.Idle -> Unit
                }
            }
            IosDivider()
            when (state) {
                is UpdateUiState.Available -> {
                    Row(Modifier.height(44.dp)) {
                        DialogButton(
                            label = "Nanti",
                            semibold = false,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onClick = onDismiss,
                        )
                        VerticalDivider()
                        DialogButton(
                            label = "Perbarui",
                            semibold = true,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onClick = { onUpdate(state.info) },
                        )
                    }
                }

                is UpdateUiState.Downloading -> {
                    DialogButton(
                        label = "Batal",
                        semibold = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        onClick = onCancelDownload,
                    )
                }

                else -> {
                    DialogButton(
                        label = "OK",
                        semibold = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        onClick = onDismiss,
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogTitle(text: String) {
    BasicText(
        text = text,
        style = NotelyText.NavTitle.copy(textAlign = TextAlign.Center),
    )
}

@Composable
private fun DialogMessage(text: String) {
    Spacer(Modifier.height(6.dp))
    BasicText(
        text = text,
        style = NotelyText.Footnote.copy(textAlign = TextAlign.Center),
    )
}

/** Bilah kemajuan tipis gaya iOS; [progress] null = belum diketahui. */
@Composable
private fun ProgressBar(progress: Float?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(NotelyColors.Separator),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress ?: 0.3f)
                .clip(RoundedCornerShape(3.dp))
                .background(NotelyColors.Blue),
        )
    }
}

@Composable
private fun DialogButton(
    label: String,
    semibold: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier.clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = label,
            style = (if (semibold) NotelyText.BodySemibold else NotelyText.Body)
                .copy(color = NotelyColors.Blue),
        )
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        Modifier
            .width(0.5.dp)
            .fillMaxHeight()
            .background(NotelyColors.Separator),
    )
}

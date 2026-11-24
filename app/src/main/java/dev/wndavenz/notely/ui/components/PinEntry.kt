package dev.wndavenz.notely.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.wndavenz.notely.ui.theme.NotelyColors
import dev.wndavenz.notely.ui.theme.NotelyText
import com.composables.icons.lucide.Delete
import com.composables.icons.lucide.Lucide
import dev.wndavenz.notely.vault.VaultStore
import kotlinx.coroutines.withTimeoutOrNull

private const val PIN_LENGTH = 4

private enum class PinPhase { ENTER, SETUP_FIRST, SETUP_CONFIRM }

/**
 * Layar PIN brankas: papan angka sendiri gaya iOS (tanpa keyboard sistem).
 *
 * Mode buat-baru ([isSetup] = true): pengguna memasukkan PIN lalu
 * mengonfirmasinya sekali lagi sebelum PIN disimpan. Mode masuk: PIN
 * diverifikasi terhadap hash tersimpan. PIN lengkap (4 digit) langsung
 * diproses otomatis, tanpa tombol tambahan.
 */
@Composable
fun PinEntryDialog(
    isSetup: Boolean,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var phase by remember {
        mutableStateOf(if (isSetup) PinPhase.SETUP_FIRST else PinPhase.ENTER)
    }
    var confirmReset by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    var pin by remember { mutableStateOf("") }
    var firstPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pin) {
        if (pin.length < PIN_LENGTH) return@LaunchedEffect
        val completed = pin
        when (phase) {
            PinPhase.ENTER -> {
                if (VaultStore.verify(context, completed)) {
                    onSuccess()
                } else {
                    error = "PIN salah"
                    pin = ""
                }
            }
            PinPhase.SETUP_FIRST -> {
                firstPin = completed
                pin = ""
                error = null
                phase = PinPhase.SETUP_CONFIRM
            }
            PinPhase.SETUP_CONFIRM -> {
                if (completed == firstPin) {
                    VaultStore.setPin(context, completed)
                    onSuccess()
                } else {
                    error = "PIN tidak sama, coba lagi"
                    pin = ""
                    firstPin = ""
                    phase = PinPhase.SETUP_FIRST
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NotelyColors.Background)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(72.dp))
            IosTextButton(
                text = "Batal",
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.Start),
            )
            // Tombol reset PIN yang tidak kelihatan: area kosong tepat di
            // atas judul, ditahan 1 detik untuk memicu reset (mode masuk
            // saja; mode buat PIN menampilkan ruang kosong yang sama agar
            // tata letak tidak bergeser).
            if (phase == PinPhase.ENTER) {
                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .height(48.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    val releasedEarly =
                                        withTimeoutOrNull(VAULT_HOLD_MS) {
                                            tryAwaitRelease()
                                        }
                                    if (releasedEarly == null) {
                                        haptic.performHapticFeedback(
                                            HapticFeedbackType.LongPress,
                                        )
                                        confirmReset = true
                                    }
                                },
                            )
                        },
                )
            } else {
                Spacer(Modifier.height(48.dp))
            }
            BasicText(
                text = when (phase) {
                    PinPhase.ENTER -> "Masukkan PIN"
                    PinPhase.SETUP_FIRST -> "Buat PIN Rahasia"
                    PinPhase.SETUP_CONFIRM -> "Konfirmasi PIN"
                },
                style = NotelyText.SectionTitle.copy(textAlign = TextAlign.Center),
            )
            Spacer(Modifier.height(8.dp))
            BasicText(
                text = "PIN 4 digit ini membuka tempat rahasia kamu.",
                style = NotelyText.Footnote.copy(textAlign = TextAlign.Center),
            )
            Spacer(Modifier.height(28.dp))

            // Indikator titik PIN.
            Row {
                for (index in 0 until PIN_LENGTH) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(14.dp)
                            .clip(RoundedCornerShape(50))
                            .then(
                                if (index < pin.length) {
                                    Modifier.background(NotelyColors.Label)
                                } else {
                                    Modifier.border(
                                        1.dp,
                                        NotelyColors.SecondaryLabel,
                                        RoundedCornerShape(50),
                                    )
                                },
                            ),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            BasicText(
                text = error ?: " ",
                style = NotelyText.Footnote.copy(
                    color = NotelyColors.Destructive,
                    textAlign = TextAlign.Center,
                ),
            )
            Spacer(Modifier.height(24.dp))

            // Papan angka 1-9, 0, hapus.
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "⌫"),
            )
            for (row in rows) {
                Row {
                    for (key in row) {
                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                                .size(72.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (key.isEmpty()) {
                                        NotelyColors.Background
                                    } else {
                                        NotelyColors.Card
                                    },
                                )
                                .clickable(enabled = key.isNotEmpty()) {
                                    error = null
                                    pin = if (key == "⌫") {
                                        pin.dropLast(1)
                                    } else if (pin.length < PIN_LENGTH) {
                                        pin + key
                                    } else {
                                        pin
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (key == "⌫") {
                                Image(
                                    imageVector = Lucide.Delete,
                                    contentDescription = "Hapus",
                                    colorFilter = ColorFilter.tint(NotelyColors.Label),
                                    modifier = Modifier.size(30.dp),
                                )
                            } else {
                                BasicText(
                                    text = key,
                                    style = NotelyText.LargeTitle.copy(fontSize = 28.sp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmReset) {
        IosConfirmDialog(
            title = "Reset PIN?",
            message = "PIN lama akan dihapus dan kamu bisa membuat PIN baru. " +
                "Catatan di dalam brankas tidak ikut terhapus.",
            confirmLabel = "Reset",
            onConfirm = {
                VaultStore.clearPin(context)
                pin = ""
                firstPin = ""
                error = null
                phase = PinPhase.SETUP_FIRST
                confirmReset = false
            },
            onDismiss = { confirmReset = false },
        )
    }
}

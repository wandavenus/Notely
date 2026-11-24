package dev.wndavenz.notely.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pin
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.X
import dev.wndavenz.notely.data.NoteEntity
import dev.wndavenz.notely.ui.theme.NotelyColors
import dev.wndavenz.notely.ui.theme.NotelyShapes
import dev.wndavenz.notely.ui.theme.NotelyText
import dev.wndavenz.notely.utils.DateFormat
import dev.wndavenz.notely.utils.NoteText
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/** Garis pemisah tipis gaya iOS, bisa diberi inden awal. */
@Composable
fun IosDivider(modifier: Modifier = Modifier, insetStart: Dp = 0.dp) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(start = insetStart)
            .height(0.5.dp)
            .background(NotelyColors.Separator),
    )
}

/** Tombol teks polos gaya iOS (mis. "Edit", "Selesai", "Batal"). */
@Composable
fun IosTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = NotelyColors.Blue,
    semibold: Boolean = false,
) {
    BasicText(
        text = text,
        style = (if (semibold) NotelyText.BodySemibold else NotelyText.Body).copy(color = color),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

/** Tombol ikon polos gaya iOS. */
@Composable
fun IosIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = NotelyColors.Blue,
    iconSize: Dp = 22.dp,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            imageVector = icon,
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(iconSize),
        )
    }
}

/**
 * Bilah atas kecil: tombol kembali di kiri, judul di tengah (opsional),
 * aksi di kanan. Dipakai layar daftar, editor, dan pengaturan.
 */
@Composable
fun IosTopBar(
    modifier: Modifier = Modifier,
    backLabel: String? = null,
    onBack: (() -> Unit)? = null,
    centerTitle: String? = null,
    trailing: @Composable () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 8.dp),
    ) {
        if (onBack != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onBack)
                    .padding(end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    imageVector = Lucide.ChevronLeft,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(NotelyColors.Blue),
                    modifier = Modifier.size(24.dp),
                )
                if (backLabel != null) {
                    BasicText(
                        text = backLabel,
                        style = NotelyText.Body.copy(color = NotelyColors.Blue),
                    )
                }
            }
        }
        if (centerTitle != null) {
            BasicText(
                text = centerTitle,
                style = NotelyText.NavTitle.copy(color = NotelyColors.Label),
                modifier = Modifier.align(Alignment.Center),
            )
        }
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            trailing()
        }
    }
}

/** Saklar gaya iOS: jalur 51x31, kenop putih 27, hijau saat menyala. */
@Composable
fun IosSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val knobOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        animationSpec = tween(durationMillis = 200),
        label = "kenop saklar",
    )
    Box(
        modifier = modifier
            .size(width = 51.dp, height = 31.dp)
            .clip(RoundedCornerShape(50))
            .background(if (checked) NotelyColors.SwitchOn else NotelyColors.SwitchOff)
            .clickable { onCheckedChange(!checked) },
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobOffset, y = 2.dp)
                .size(27.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White),
        )
    }
}

/** Kolom pencarian gaya iOS (membulat, ikon kaca pembesar, tombol hapus). */
@Composable
fun IosSearchField(
    state: TextFieldState,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        state = state,
        modifier = modifier.fillMaxWidth(),
        textStyle = NotelyText.Body.copy(color = NotelyColors.Label),
        cursorBrush = SolidColor(NotelyColors.Blue),
        lineLimits = TextFieldLineLimits.SingleLine,
        decorator = { innerTextField ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(NotelyShapes.SearchRadius))
                    .background(NotelyColors.SearchFill)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    imageVector = Lucide.Search,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(NotelyColors.SecondaryLabel),
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Box(Modifier.weight(1f)) {
                    if (state.text.isEmpty()) {
                        BasicText(
                            text = placeholder,
                            style = NotelyText.Body.copy(color = NotelyColors.SecondaryLabel),
                        )
                    }
                    innerTextField()
                }
                if (state.text.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { state.clearText() }
                            .padding(2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            imageVector = Lucide.X,
                            contentDescription = "Hapus pencarian",
                            colorFilter = ColorFilter.tint(NotelyColors.SecondaryLabel),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        },
    )
}

/** Kepala seksi daftar catatan; seksi sematan memakai ikon pin biru. */
@Composable
fun NoteSectionHeader(title: String, pinned: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(start = 4.dp, top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (pinned) {
            Image(
                imageVector = Lucide.Pin,
                contentDescription = null,
                colorFilter = ColorFilter.tint(NotelyColors.Blue),
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        BasicText(
            text = title,
            style = NotelyText.SectionTitle.copy(color = NotelyColors.Label),
        )
    }
}

/** Kartu satu catatan pada daftar: judul tebal, jam + pratinjau satu baris. */
@Composable
fun NoteCard(
    note: NoteEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val subtitle = buildAnnotatedString {
        withStyle(SpanStyle(color = NotelyColors.SecondaryLabel)) {
            append(DateFormat.time(note.updatedAt))
            append("  ")
            append(NoteText.previewOf(note.body))
        }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NotelyShapes.NoteCardRadius))
            .background(NotelyColors.Card)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
    ) {
        BasicText(
            text = NoteText.titleOf(note.body),
            style = NotelyText.BodySemibold.copy(color = NotelyColors.Label),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(3.dp))
        BasicText(
            text = subtitle,
            style = NotelyText.Footnote,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Kartu grup membulat gaya iOS untuk baris-baris berkelompok (radius 9 dp). */
@Composable
fun IosGroupCard(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NotelyShapes.FolderCardRadius))
            .background(NotelyColors.Card),
        content = content,
    )
}

/** Teks keadaan kosong di tengah area daftar. */
@Composable
fun EmptyStateText(text: String, modifier: Modifier = Modifier) {
    BasicText(
        text = text,
        style = NotelyText.Callout.copy(
            color = NotelyColors.SecondaryLabel,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
    )
}

/** Lama tahan (ms) pada teks tengah bilah bawah untuk membuka brankas. */
const val VAULT_HOLD_MS = 2_000L

/** Bilah bawah: slot kiri, teks jumlah di tengah, slot kanan. */
@Composable
fun IosBottomBar(
    modifier: Modifier = Modifier,
    centerText: String? = null,
    onCenterHold: (() -> Unit)? = null,
    leading: @Composable () -> Unit = {},
    trailing: @Composable () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    Column(modifier = modifier.fillMaxWidth()) {
        IosDivider()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 10.dp),
        ) {
            Box(Modifier.align(Alignment.CenterStart)) { leading() }
            if (centerText != null) {
                val holdModifier = if (onCenterHold != null) {
                    Modifier.pointerInput(onCenterHold) {
                        awaitPointerEventScope {
                            while (true) {
                                val first = awaitPointerEvent(PointerEventPass.Initial)
                                if (first.changes.none { it.pressed }) continue
                                var released = false
                                try {
                                    withTimeout(VAULT_HOLD_MS) {
                                        while (true) {
                                            val event =
                                                awaitPointerEvent(PointerEventPass.Initial)
                                            if (event.changes.none { it.pressed }) {
                                                released = true
                                                break
                                            }
                                        }
                                    }
                                } catch (e: TimeoutCancellationException) {
                                    // Masih ditekan saat batas waktu habis.
                                }
                                if (!released) {
                                    onCenterHold()
                                    haptic.performHapticFeedback(
                                        HapticFeedbackType.LongPress,
                                    )
                                    // Tunggu sampai jari diangkat.
                                    while (true) {
                                        val event =
                                            awaitPointerEvent(PointerEventPass.Initial)
                                        if (event.changes.none { it.pressed }) break
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Modifier
                }
                BasicText(
                    text = centerText,
                    style = NotelyText.Caption.copy(color = NotelyColors.Label),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .then(holdModifier)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
            Box(Modifier.align(Alignment.CenterEnd)) { trailing() }
        }
    }
}

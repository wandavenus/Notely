package dev.wndavenz.notely.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.wndavenz.notely.ui.theme.NotelyColors
import dev.wndavenz.notely.ui.theme.NotelyShapes
import dev.wndavenz.notely.ui.theme.NotelyText

/** Satu baris aksi pada lembar aksi gaya iOS. */
data class SheetAction(
    val label: String,
    val color: Color = NotelyColors.Blue,
    val onClick: () -> Unit,
)

/** Lembar aksi dari bawah gaya iOS: grup aksi membulat + tombol Batal terpisah. */
@Composable
fun IosActionSheet(
    actions: List<SheetAction>,
    onDismiss: () -> Unit,
    title: String? = null,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(onClick = onDismiss),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 28.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(NotelyShapes.SheetRadius))
                    .background(NotelyColors.SheetBackground),
            ) {
                if (title != null) {
                    BasicText(
                        text = title,
                        style = NotelyText.Footnote.copy(textAlign = TextAlign.Center),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                    IosDivider()
                }
                actions.forEachIndexed { index, action ->
                    if (index > 0) IosDivider()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clickable {
                                action.onClick()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = action.label,
                            style = NotelyText.Body.copy(color = action.color),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(NotelyShapes.SheetRadius))
                    .background(NotelyColors.SheetBackground)
                    .height(56.dp)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "Batal",
                    style = NotelyText.BodySemibold.copy(color = NotelyColors.Label),
                )
            }
            }
        }
    }
}

/** Dialog peringatan gaya iOS dengan dua tombol: Batal dan konfirmasi. */
@Composable
fun IosConfirmDialog(
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    message: String? = null,
    destructive: Boolean = true,
) {
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
                BasicText(
                    text = title,
                    style = NotelyText.NavTitle.copy(textAlign = TextAlign.Center),
                )
                if (message != null) {
                    Spacer(Modifier.height(6.dp))
                    BasicText(
                        text = message,
                        style = NotelyText.Footnote.copy(textAlign = TextAlign.Center),
                    )
                }
            }
            IosDivider()
            Row(Modifier.height(44.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "Batal",
                        style = NotelyText.Body.copy(color = NotelyColors.Blue),
                    )
                }
                Box(
                    Modifier
                        .width(0.5.dp)
                        .fillMaxHeight()
                        .background(NotelyColors.Separator),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(onClick = onConfirm),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = confirmLabel,
                        style = NotelyText.BodySemibold.copy(
                            color = if (destructive) {
                                NotelyColors.Destructive
                            } else {
                                NotelyColors.Blue
                            },
                        ),
                    )
                }
            }
        }
    }
}

/** Dialog masukan teks satu baris gaya iOS (mis. membuat folder baru). */
@Composable
fun IosTextInputDialog(
    title: String,
    placeholder: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTextFieldState()
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
                BasicText(
                    text = title,
                    style = NotelyText.NavTitle.copy(textAlign = TextAlign.Center),
                )
                Spacer(Modifier.height(12.dp))
                BasicTextField(
                    state = state,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = NotelyText.Body,
                    cursorBrush = SolidColor(NotelyColors.Blue),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    decorator = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NotelyColors.Card)
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                        ) {
                            if (state.text.isEmpty()) {
                                BasicText(
                                    text = placeholder,
                                    style = NotelyText.Body.copy(
                                        color = NotelyColors.TertiaryLabel,
                                    ),
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }
            IosDivider()
            Row(Modifier.height(44.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "Batal",
                        style = NotelyText.Body.copy(color = NotelyColors.Blue),
                    )
                }
                Box(
                    Modifier
                        .width(0.5.dp)
                        .fillMaxHeight()
                        .background(NotelyColors.Separator),
                )
                val enabled = state.text.isNotBlank()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(enabled = enabled) {
                            onConfirm(state.text.toString().trim())
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = confirmLabel,
                        style = NotelyText.BodySemibold.copy(
                            color = if (enabled) {
                                NotelyColors.Blue
                            } else {
                                NotelyColors.TertiaryLabel
                            },
                        ),
                    )
                }
            }
        }
    }
}

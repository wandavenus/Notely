package dev.wndavenz.notely.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.ChevronUp
import com.composables.icons.lucide.Ellipsis
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import dev.wndavenz.notely.documents.DocumentFile
import dev.wndavenz.notely.documents.ReaderPrefs
import dev.wndavenz.notely.ui.DocumentReaderViewModel
import dev.wndavenz.notely.ui.components.EmptyStateText
import dev.wndavenz.notely.ui.components.IosActionSheet
import dev.wndavenz.notely.ui.components.IosIconButton
import dev.wndavenz.notely.ui.components.IosSearchField
import dev.wndavenz.notely.ui.components.IosTextInputDialog
import dev.wndavenz.notely.ui.components.IosTopBar
import dev.wndavenz.notely.ui.components.SheetAction
import dev.wndavenz.notely.ui.theme.NotelyColors
import dev.wndavenz.notely.ui.theme.NotelyText
import dev.wndavenz.notely.ui.viewModelFactory
import dev.wndavenz.notely.utils.Markdown
import dev.wndavenz.notely.utils.MarkdownInline
import dev.wndavenz.notely.utils.MdBlock
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Layar baca dokumen: .md dirender sebagai Markdown, .txt sebagai teks polos
 * bernomor baris. Menu titik tiga: Cari, Pergi ke Baris, Hanya Baca (mati =
 * mode edit sumber dengan simpan otomatis ke file asli), Pembungkus Kata.
 */
@Composable
fun DocumentReaderScreen(document: DocumentFile, onBack: () -> Unit) {
    val viewModel: DocumentReaderViewModel = viewModel(
        key = "reader-${document.path}",
        factory = viewModelFactory { DocumentReaderViewModel(document) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    var readOnly by remember { mutableStateOf(true) }
    var wordWrap by remember { mutableStateOf(true) }
    // Gulir horizontal BERSAMA untuk mode bentang Markdown: semua blok
    // digeser satu offset yang sama (scroll per blok bikin cuma blok yang
    // disentuh yang bergerak). Lebar isi terlebar dilaporkan tiap blok.
    // Offset disimpan sebagai State dan dibaca di FASE LAYOUT (di dalam
    // lambda offset tiap blok), bukan sebagai parameter komposisi —
    // supaya geseran tidak memicu komposisi ulang semua blok (itu yang
    // bikin geser menyamping terasa kaku; gulir vertikal LazyColumn
    // mulus karena memang bekerja di fase layout).
    val mdScrollXState = remember { mutableFloatStateOf(0f) }
    var mdMaxWidthPx by remember { mutableIntStateOf(0) }
    var mdViewportWidthPx by remember { mutableIntStateOf(0) }
    var mdFlingJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    LaunchedEffect(wordWrap, document.path) {
        mdFlingJob?.cancel()
        mdScrollXState.floatValue = 0f
        mdMaxWidthPx = 0
    }
    var showMenu by remember { mutableStateOf(false) }
    var showGoToLine by remember { mutableStateOf(false) }
    var showTextSizeDialog by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }

    // Ukuran teks viewer (pilihan menu titik tiga), tersimpan antar sesi.
    val context = LocalContext.current
    var textScale by remember { mutableStateOf(ReaderPrefs.textScale(context)) }

    val listState = rememberLazyListState()
    val txtScrollState = rememberScrollState()
    val editorScrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val blocksWithLines = remember(state.text, document.isMarkdown, readOnly) {
        // Pas mode edit, hasil render tidak ditampilkan: lewati parse supaya
        // simpan otomatis tidak membekukan editor tiap beberapa detik.
        if (document.isMarkdown && readOnly && state.loaded && !state.failed) {
            Markdown.parseWithLines(state.text)
        } else {
            emptyList()
        }
    }
    val textLines = remember(state.text) { state.text.split('\n') }

    // --- Cari ---
    val searchState = rememberTextFieldState()
    val query = searchState.text.toString()
    var currentMatch by remember { mutableStateOf(0) }
    val editing = !readOnly
    val matchItems: List<Int> = remember(query, blocksWithLines, textLines, editing) {
        if (query.isBlank() || !state.loaded || state.failed) {
            emptyList()
        } else if (!editing && document.isMarkdown) {
            blocksWithLines.indices.filter { index ->
                blockPlainText(blocksWithLines[index].first)
                    .contains(query, ignoreCase = true)
            }
        } else {
            textLines.indices.filter { textLines[it].contains(query, ignoreCase = true) }
        }
    }
    /** Lompat ke baris TXT: mode bungkus = item LazyColumn; mode bentang =
     *  gulir piksel setinggi baris itu (teks satu node, bukan per baris). */
    fun scrollToTxtLine(lineIndex: Int) {
        scope.launch {
            if (wordWrap) {
                listState.animateScrollToItem(lineIndex)
            } else {
                val px = with(density) { (24.sp * textScale * lineIndex).toPx() }
                txtScrollState.animateScrollTo(px.toInt())
            }
        }
    }

    LaunchedEffect(query) {
        currentMatch = 0
        if (!editing && matchItems.isNotEmpty()) {
            if (!document.isMarkdown && !wordWrap) {
                scrollToTxtLine(matchItems[0])
            } else {
                listState.animateScrollToItem(matchItems[0])
            }
        }
    }

    fun goToMatch(delta: Int) {
        if (matchItems.isEmpty()) return
        currentMatch = (currentMatch + delta).mod(matchItems.size)
        if (!editing) {
            if (!document.isMarkdown && !wordWrap) {
                scrollToTxtLine(matchItems[currentMatch])
            } else {
                scope.launch { listState.animateScrollToItem(matchItems[currentMatch]) }
            }
        }
    }

    fun goToLine(lineNumber: Int) {
        when {
            editing -> {
                val px = with(density) {
                    (20.sp * textScale * (lineNumber - 1).coerceAtLeast(0)).toPx()
                }
                scope.launch { editorScrollState.animateScrollTo(px.toInt()) }
            }
            document.isMarkdown -> {
                val index = blocksWithLines.indexOfLast { it.second <= lineNumber }
                scope.launch { listState.animateScrollToItem(if (index >= 0) index else 0) }
            }
            else -> {
                val target = (lineNumber - 1).coerceIn(0, (textLines.size - 1).coerceAtLeast(0))
                scrollToTxtLine(target)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        IosTopBar(
            backLabel = "Dokumen",
            onBack = onBack,
            centerTitle = document.name,
            titleMarquee = true,
            trailing = {
                IosIconButton(
                    icon = Lucide.Ellipsis,
                    contentDescription = "Menu dokumen",
                    onClick = { showMenu = true },
                )
            },
        )
        // Pembatas di ujung bawah navbar, gaya sama seperti pembatas
        // nomor baris: 0,3 dp, putih 70%.
        Box(
            Modifier
                .fillMaxWidth()
                .height(0.3.dp)
                .background(Color.White.copy(alpha = 0.7f)),
        )


        if (searchOpen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    IosSearchField(state = searchState, placeholder = "Cari di dokumen")
                }
                if (query.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    BasicText(
                        text = if (matchItems.isEmpty()) {
                            "0/0"
                        } else {
                            "${currentMatch + 1}/${matchItems.size}"
                        },
                        style = NotelyText.Footnote,
                    )
                    IosIconButton(
                        icon = Lucide.ChevronUp,
                        contentDescription = "Hasil sebelumnya",
                        onClick = { goToMatch(-1) },
                    )
                    IosIconButton(
                        icon = Lucide.ChevronDown,
                        contentDescription = "Hasil berikutnya",
                        onClick = { goToMatch(1) },
                    )
                }
                IosIconButton(
                    icon = Lucide.X,
                    contentDescription = "Tutup pencarian",
                    onClick = {
                        searchOpen = false
                        searchState.clearText()
                    },
                )
            }
        }

        when {
            !state.loaded -> EmptyStateText("Memuat…")

            state.failed -> EmptyStateText(
                "File tidak bisa dibaca. Mungkin sudah dipindahkan atau dihapus.",
            )

            editing -> DocumentSourceEditor(
                viewModel = viewModel,
                initialText = state.text,
                scrollState = editorScrollState,
                textScale = textScale,
                modifier = Modifier.weight(1f),
            )

            document.isMarkdown -> Box(
                modifier = Modifier
                    .weight(1f)
                    .onSizeChanged { size ->
                        mdViewportWidthPx = size.width -
                            with(density) { 40.dp.toPx() }.toInt()
                    }
                    .pointerInput(wordWrap) {
                        if (!wordWrap) {
                            val velocityTracker = VelocityTracker()
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    mdFlingJob?.cancel()
                                    velocityTracker.resetTracking()
                                },
                                onDragEnd = {
                                    // Lemparan (fling) seperti gulir vertikal:
                                    // habis dilepas, isi meluncur melambat
                                    // sendiri mengikuti kecepatan jari.
                                    val velocityX = velocityTracker
                                        .calculateVelocity()
                                        .x
                                    if (abs(velocityX) > 60f) {
                                        mdFlingJob = scope.launch {
                                            var velocity = -velocityX
                                            var lastFrame = withFrameNanos { it }
                                            while (abs(velocity) > 24f) {
                                                val now = withFrameNanos { it }
                                                val dt = (now - lastFrame) / 1e9f
                                                lastFrame = now
                                                if (dt <= 0f) continue
                                                val maxScroll = (
                                                    mdMaxWidthPx - mdViewportWidthPx
                                                    )
                                                    .coerceAtLeast(0)
                                                    .toFloat()
                                                val next = mdScrollXState.floatValue +
                                                    velocity * dt
                                                mdScrollXState.floatValue =
                                                    next.coerceIn(0f, maxScroll)
                                                if (next != mdScrollXState.floatValue) {
                                                    break
                                                }
                                                velocity *= exp(-3.2f * dt)
                                            }
                                        }
                                    }
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    velocityTracker.addPosition(
                                        change.uptimeMillis,
                                        change.position,
                                    )
                                    val maxScroll = (mdMaxWidthPx - mdViewportWidthPx)
                                        .coerceAtLeast(0)
                                        .toFloat()
                                    mdScrollXState.floatValue =
                                        (mdScrollXState.floatValue - dragAmount)
                                            .coerceIn(0f, maxScroll)
                                    change.consume()
                                },
                            )
                        }
                    },
            ) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(top = 12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                ) {
                    items(blocksWithLines.size) { index ->
                        MarkdownBlock(
                            block = blocksWithLines[index].first,
                            query = query,
                            wordWrap = wordWrap,
                            textScale = textScale,
                            scrollXState = mdScrollXState,
                            onContentWidth = { width ->
                                // Batas waras: ukuran rusak (mis. tak
                                // terbatas) jangan sampai jadi batas geser.
                                if (width in 1..500_000 && width > mdMaxWidthPx) {
                                    mdMaxWidthPx = width
                                }
                            },
                        )
                    }
                    if (state.truncated) {
                        item { TruncatedNote() }
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }

            else -> if (wordWrap) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(top = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 0.dp, end = 12.dp),
                ) {
                    items(textLines.size) { index ->
                        NumberedLineRow(number = index + 1, wrap = wordWrap) { softWrap, modifier ->
                            BasicText(
                                text = addHighlights(
                                    AnnotatedString(textLines[index].ifEmpty { " " }),
                                    query,
                                ),
                                style = scaledTextStyle(NotelyText.EditorBody, textScale),
                                softWrap = softWrap,
                                modifier = modifier,
                            )
                        }
                    }
                    if (state.truncated) {
                        item { TruncatedNote() }
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            } else {
                TxtNoWrapView(
                    lines = textLines,
                    query = query,
                    textScale = textScale,
                    truncated = state.truncated,
                    scrollState = txtScrollState,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }

    if (showMenu) {
        IosActionSheet(
            actions = listOf(
                SheetAction(label = "Cari") {
                    searchOpen = true
                    showMenu = false
                },
                SheetAction(label = "Pergi ke Baris") {
                    showGoToLine = true
                    showMenu = false
                },
                SheetAction(label = "Ukuran Teks") {
                    showTextSizeDialog = true
                    showMenu = false
                },
                SheetAction(
                    label = if (readOnly) "Hanya Baca ✓" else "Hanya Baca",
                    color = NotelyColors.Label,
                ) {
                    // Masuk mode edit: editor diisi teks sumber saat ini
                    // (lewat initialText); keluar: editor menyimpan otomatis.
                    readOnly = !readOnly
                    showMenu = false
                },
                SheetAction(
                    label = if (wordWrap) "Pembungkus Kata ✓" else "Pembungkus Kata",
                    color = NotelyColors.Label,
                ) {
                    wordWrap = !wordWrap
                    showMenu = false
                },
            ),
            onDismiss = { showMenu = false },
        )
    }

    if (showTextSizeDialog) {
        TextSizeDialog(
            scale = textScale,
            onScaleChange = { newScale ->
                textScale = newScale
                ReaderPrefs.setTextScale(context, newScale)
            },
            onDismiss = { showTextSizeDialog = false },
        )
    }

    if (showGoToLine) {
        IosTextInputDialog(
            title = "Pergi ke Baris",
            placeholder = "Nomor baris",
            confirmLabel = "Pergi",
            onConfirm = { input ->
                input.toIntOrNull()?.let { goToLine(it) }
                showGoToLine = false
            },
            onDismiss = { showGoToLine = false },
        )
    }
}

/** Teks polos satu blok untuk pencarian (blok kode memakai isi kodenya). */
private fun blockPlainText(block: MdBlock): String = when (block) {
    is MdBlock.Heading -> block.text
    is MdBlock.Paragraph -> block.text
    is MdBlock.Bullet -> block.text
    is MdBlock.Numbered -> block.text
    is MdBlock.Code -> block.text
    is MdBlock.Quote -> block.text
    MdBlock.Divider -> ""
}

/**
 * Editor sumber (mode Hanya Baca mati): nomor baris + garis putih pembatas +
 * kolom teks monospace; perubahan tersimpan otomatis ke file asli.
 */
@Composable
private fun DocumentSourceEditor(
    viewModel: DocumentReaderViewModel,
    initialText: String,
    scrollState: androidx.compose.foundation.ScrollState,
    textScale: Float,
    modifier: Modifier = Modifier,
) {
    val editorState = rememberTextFieldState()
    var initialized by remember { mutableStateOf(false) }
    var lineCount by remember { mutableStateOf(1) }

    LaunchedEffect(Unit) {
        editorState.setTextAndPlaceCursorAtEnd(initialText)
        lineCount = initialText.count { it == '\n' } + 1
        initialized = true
    }
    LaunchedEffect(editorState, initialized) {
        if (initialized) {
            snapshotFlow { editorState.text.toString() }.collect { text ->
                lineCount = text.count { it == '\n' } + 1
                viewModel.onEdited(text)
            }
        }
    }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { viewModel.saveNow(editorState.text.toString()) }
    }

    Box(modifier = modifier.verticalScroll(scrollState)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 0.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
                .drawBehind {
                    // Garis vertikal pembatas nomor baris <-> teks (0,3 dp,
                    // putih 70% — lebih tipis & gelap dr putih penuh),
                    // setinggi isi editor yang sebenarnya (digambar belakangan
                    // supaya tidak perlu menebak tinggi konten).
                    val x = 30.dp.toPx() + 8.dp.toPx()
                    drawLine(
                        color = Color.White.copy(alpha = 0.7f),
                        start = androidx.compose.ui.geometry.Offset(x, 0f),
                        end = androidx.compose.ui.geometry.Offset(x, size.height),
                        strokeWidth = 0.3.dp.toPx(),
                    )
                },
        ) {
            // Semua nomor baris dalam SATU teks (rata kanan per baris):
            // satu node saja, bukan satu BasicText per baris — editor file
            // besar jadi jauh lebih ringan pas mengetik.
            val numbersText = remember(lineCount) {
                buildString {
                    for (number in 1..lineCount) {
                        if (number > 1) append('\n')
                        append(number)
                    }
                }
            }
            BasicText(
                text = numbersText,
                style = NotelyText.Caption.copy(
                    color = NotelyColors.TertiaryLabel,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.5.sp * textScale,
                    lineHeight = 20.sp * textScale,
                    textAlign = TextAlign.End,
                ),
                modifier = Modifier.width(30.dp),
            )
            Spacer(Modifier.width(19.dp))
            BasicTextField(
                state = editorState,
                modifier = Modifier.weight(1f),
                textStyle = NotelyText.Callout.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.5.sp * textScale,
                    lineHeight = 20.sp * textScale,
                ),
                cursorBrush = SolidColor(NotelyColors.Blue),
                lineLimits = TextFieldLineLimits.MultiLine(),
            )
        }
    }
}

/**
 * Dialog "Ukuran Teks": slider geser bebas 0,5x–1,4x dengan pratinjau
 * langsung + label persen; perubahan tersimpan seketika.
 */
@Composable
private fun TextSizeDialog(
    scale: Float,
    onScaleChange: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val minScale = 0.5f
    val maxScale = 1.4f
    val density = LocalDensity.current
    var trackWidthPx by remember { mutableStateOf(0) }
    val fraction = ((scale - minScale) / (maxScale - minScale)).coerceIn(0f, 1f)

    fun updateFromX(x: Float) {
        if (trackWidthPx > 0) {
            val f = (x / trackWidthPx).coerceIn(0f, 1f)
            onScaleChange(minScale + f * (maxScale - minScale))
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(300.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(NotelyColors.SheetBackground)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicText(
                text = "Ukuran Teks",
                style = NotelyText.NavTitle.copy(textAlign = TextAlign.Center),
            )
            Spacer(Modifier.height(16.dp))
            BasicText(
                text = "Contoh teks dokumen",
                style = scaledTextStyle(NotelyText.EditorBody, scale),
                maxLines = 1,
            )
            Spacer(Modifier.height(4.dp))
            BasicText(
                text = "${(scale * 100).roundToInt()}%",
                style = NotelyText.Footnote,
            )
            Spacer(Modifier.height(18.dp))
            // Rel slider: ketuk atau geser di mana pun sepanjang rel.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .onSizeChanged { trackWidthPx = it.width }
                    .pointerInput(trackWidthPx) {
                        coroutineScope {
                            launch {
                                detectTapGestures { offset -> updateFromX(offset.x) }
                            }
                            launch {
                                detectDragGestures { change, _ ->
                                    updateFromX(change.position.x)
                                    change.consume()
                                }
                            }
                        }
                    },
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(NotelyColors.Separator),
                )
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .width(with(density) { (trackWidthPx * fraction).toDp() })
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(NotelyColors.Blue),
                )
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .offset {
                            IntOffset(
                                x = (trackWidthPx * fraction).roundToInt() -
                                    with(density) { 12.dp.roundToPx() },
                                y = 0,
                            )
                        }
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                )
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onScaleChange(1.0f) },
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "Atur Ulang",
                        style = NotelyText.Body.copy(color = NotelyColors.Blue),
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "Selesai",
                        style = NotelyText.BodySemibold.copy(color = NotelyColors.Blue),
                    )
                }
            }
        }
    }
}

/**
 * Viewer TXT mode bentang (Pembungkus Kata mati): teks digambar sebagai
 * SATU node supaya gulir horizontal menggeser semua baris serentak.
 * Nomor baris ikut di dalam aliran gulir vertikal yang sama dengan teks
 * (BUKAN digeser manual lewat offset — cara lama bikin teks nomor cuma
 * kelayout setinggi satu layar, jadi nomor berhenti di tengah file),
 * dan pembatas digambar di belakang baris setinggi isi penuh.
 */
@Composable
private fun TxtNoWrapView(
    lines: List<String>,
    query: String,
    textScale: Float,
    truncated: Boolean,
    scrollState: androidx.compose.foundation.ScrollState,
    modifier: Modifier = Modifier,
) {
    val bodyStyle = scaledTextStyle(NotelyText.EditorBody, textScale)
    val fullText = remember(lines, query) {
        val joined = lines.joinToString("\n")
        addHighlights(AnnotatedString(joined.ifEmpty { " " }), query)
    }
    val numbersText = remember(lines.size) {
        (1..lines.size).joinToString("\n")
    }
    val horizontalState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(end = 12.dp),
    ) {
        // Napas di ATAS isi teks (di dalam area gulir, bukan pita diam
        // di bawah pembatas navbar).
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.drawBehind {
                // Pembatas vertikal nomor <-> teks, setinggi seluruh isi.
                val x = 30.dp.toPx() + 8.dp.toPx()
                drawLine(
                    color = Color.White.copy(alpha = 0.7f),
                    start = androidx.compose.ui.geometry.Offset(x, 0f),
                    end = androidx.compose.ui.geometry.Offset(x, size.height),
                    strokeWidth = 0.3.dp.toPx(),
                )
            },
        ) {
            BasicText(
                text = numbersText,
                style = bodyStyle.copy(
                    color = NotelyColors.TertiaryLabel,
                    fontSize = 13.sp * textScale,
                    textAlign = TextAlign.End,
                ),
                modifier = Modifier.width(30.dp),
            )
            Spacer(Modifier.width(19.dp))
            Box(
                Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalState),
            ) {
                BasicText(text = fullText, style = bodyStyle, softWrap = false)
            }
        }
        if (truncated) {
            TruncatedNote()
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun TruncatedNote() {
    Spacer(Modifier.height(16.dp))
    BasicText(
        text = "File terlalu besar — hanya bagian awal yang ditampilkan.",
        style = NotelyText.Footnote,
    )
}

/** Kalikan ukuran huruf + tinggi baris satu gaya teks dengan skala Ukuran Teks. */
private fun scaledTextStyle(style: androidx.compose.ui.text.TextStyle, scale: Float) =
    style.copy(fontSize = style.fontSize * scale, lineHeight = style.lineHeight * scale)

/**
 * Teks prose Markdown yang menghormati Pembungkus Kata: nyala = teks
 * membungkus normal; mati = softWrap mati — teks memanjang selebar
 * aslinya dan ikut digeser offset bersama bloknya (lihat MarkdownBlock).
 */
@Composable
private fun MdWrapText(
    text: AnnotatedString,
    style: androidx.compose.ui.text.TextStyle,
    wordWrap: Boolean,
    modifier: Modifier = Modifier,
) {
    BasicText(text = text, style = style, softWrap = wordWrap, modifier = modifier)
}

@Composable
private fun MarkdownBlock(
    block: MdBlock,
    query: String,
    wordWrap: Boolean,
    textScale: Float,
    scrollXState: androidx.compose.runtime.State<Float> =
        androidx.compose.runtime.mutableFloatStateOf(0f),
    onContentWidth: (Int) -> Unit = {},
) {
    if (wordWrap) {
        MarkdownBlockContent(block, query, wordWrap, textScale)
    } else {
        // Mode bentang: blok diukur selebar isi aslinya lalu digeser
        // dengan offset bersama satu dokumen (dipotong di tepi item),
        // jadi semua blok bergerak serentak pas digeser menyamping.
        // Nilai offset dibaca di dalam lambda offset (fase layout) —
        // geseran cuma menata ulang posisi, TANPA komposisi ulang,
        // makanya bisa semulus gulir vertikal.
        Box(
            Modifier
                .fillMaxWidth()
                .clipToBounds(),
        ) {
            Column(
                Modifier
                    // WAJIB rata kiri (Start): bawaan wrapContentWidth itu
                    // Center — blok yang lebih lebar dari layar jadi
                    // ketengah dan yang kelihatan cuma potongan tengahnya,
                    // awal barisnya hilang (tampilan "ngacak").
                    .wrapContentWidth(unbounded = true, align = Alignment.Start)
                    .offset {
                        IntOffset(
                            -scrollXState.value.coerceAtLeast(0f).roundToInt(),
                            0,
                        )
                    }
                    .onSizeChanged { onContentWidth(it.width) },
            ) {
                MarkdownBlockContent(block, query, wordWrap, textScale)
            }
        }
    }
}

@Composable
private fun MarkdownBlockContent(block: MdBlock, query: String, wordWrap: Boolean, textScale: Float) {
    val bodyStyle = scaledTextStyle(NotelyText.EditorBody, textScale)
    when (block) {
        is MdBlock.Heading -> {
            Spacer(Modifier.height(if (block.level == 1) 10.dp else 16.dp))
            MdWrapText(
                text = inlineMarkdown(block.text, query),
                style = when (block.level) {
                    1 -> NotelyText.EditorTitle.copy(
                        fontSize = 26.sp * textScale,
                        lineHeight = 32.sp * textScale,
                    )
                    2 -> scaledTextStyle(NotelyText.EditorTitle, textScale)
                    else -> NotelyText.BodySemibold.copy(
                        fontSize = 18.sp * textScale,
                        lineHeight = 24.sp * textScale,
                    )
                },
                wordWrap = wordWrap,
            )
            Spacer(Modifier.height(6.dp))
        }

        is MdBlock.Paragraph -> {
            MdWrapText(
                text = inlineMarkdown(block.text, query),
                style = bodyStyle,
                wordWrap = wordWrap,
            )
            Spacer(Modifier.height(10.dp))
        }

        is MdBlock.Bullet -> {
            val marker = when (block.checked) {
                true -> "☑  "
                false -> "☐  "
                null -> "•  "
            }
            if (wordWrap) {
                Row(Modifier.padding(start = 4.dp, bottom = 6.dp)) {
                    BasicText(
                        text = marker,
                        style = bodyStyle.copy(color = NotelyColors.Blue),
                    )
                    MdWrapText(
                        text = inlineMarkdown(block.text, query),
                        style = bodyStyle,
                        wordWrap = wordWrap,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                // Mode bentang: penanda + isi digabung SATU teks. Pola lama
                // (Row + weight) rusak pas diukur selebar isi asli — teks
                // itemnya hilang, tinggal penandanya doang.
                BasicText(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = NotelyColors.Blue)) {
                            append(marker)
                        }
                        append(inlineMarkdown(block.text, query))
                    },
                    style = bodyStyle,
                    softWrap = false,
                    modifier = Modifier.padding(start = 4.dp),
                )
                Spacer(Modifier.height(6.dp))
            }
        }

        is MdBlock.Numbered -> {
            if (wordWrap) {
                Row(Modifier.padding(start = 4.dp, bottom = 6.dp)) {
                    BasicText(
                        text = "${block.number}.  ",
                        style = bodyStyle.copy(color = NotelyColors.Blue),
                    )
                    MdWrapText(
                        text = inlineMarkdown(block.text, query),
                        style = bodyStyle,
                        wordWrap = wordWrap,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                BasicText(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = NotelyColors.Blue)) {
                            append("${block.number}.  ")
                        }
                        append(inlineMarkdown(block.text, query))
                    },
                    style = bodyStyle,
                    softWrap = false,
                    modifier = Modifier.padding(start = 4.dp),
                )
                Spacer(Modifier.height(6.dp))
            }
        }

        is MdBlock.Code -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(NotelyColors.Card)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                if (block.language.isNotEmpty()) {
                    BasicText(
                        text = block.language,
                        style = NotelyText.Caption.copy(color = NotelyColors.TertiaryLabel),
                    )
                    Spacer(Modifier.height(6.dp))
                }
                val codeLines = remember(block.text, block.language, query) {
                    highlightedCodeLines(block.text, block.language, query)
                }
                // Kode digambar sebagai SATU node teks (baris digabung \n)
                // supaya gulir horizontal mode bentang menggeser semua
                // baris serentak; nomor baris juga satu teks rata kanan.
                val combinedCode = remember(codeLines) {
                    buildAnnotatedString {
                        codeLines.forEachIndexed { index, line ->
                            if (index > 0) append('\n')
                            append(line)
                        }
                    }
                }
                val codeNumbers = remember(codeLines.size) {
                    (1..codeLines.size).joinToString("\n")
                }
                val codeScrollState = rememberScrollState()
                Row(Modifier.height(IntrinsicSize.Max)) {
                    BasicText(
                        text = codeNumbers,
                        style = NotelyText.Caption.copy(
                            color = NotelyColors.TertiaryLabel,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp * textScale,
                            lineHeight = 19.sp * textScale,
                            textAlign = TextAlign.End,
                        ),
                        modifier = Modifier.width(26.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .width(0.3.dp)
                            .fillMaxHeight()
                            .background(Color.White.copy(alpha = 0.7f)),
                    )
                    Spacer(Modifier.width(10.dp))
                    BasicText(
                        text = combinedCode,
                        style = NotelyText.Callout.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.5.sp * textScale,
                            lineHeight = 19.sp * textScale,
                        ),
                        softWrap = wordWrap,
                        modifier = if (wordWrap) {
                            Modifier
                        } else {
                            Modifier.horizontalScroll(codeScrollState)
                        },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        is MdBlock.Quote -> {
            Row(Modifier.padding(bottom = 10.dp)) {
                Box(
                    Modifier
                        .width(3.dp)
                        .height(24.dp)
                        .background(NotelyColors.Blue),
                )
                Spacer(Modifier.width(12.dp))
                MdWrapText(
                    text = inlineMarkdown(block.text, query),
                    style = bodyStyle.copy(color = NotelyColors.SecondaryLabel),
                    wordWrap = wordWrap,
                    // Mode bentang tanpa weight: lebar Row tak terbatas,
                    // weight bikin ukuran teks item rusak (lihat Bullet).
                    modifier = if (wordWrap) Modifier.weight(1f) else Modifier,
                )
            }
        }

        MdBlock.Divider -> {
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(NotelyColors.Separator),
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** Palet pewarnaan sintaks blok kode (tema gelap, kontras di atas kartu). */
private object CodeColors {
    val Keyword = Color(0xFFFF7AB6)
    val String = Color(0xFFA6D189)
    val Number = Color(0xFFEF9F76)
    val Comment = Color(0xFF7C7F93)
    val Function = Color(0xFF82AAFF)
    val Type = Color(0xFFFFCB6B)
    val Annotation = Color(0xFF89DCEB)
    val Plain = Color(0xFFE6E9EF)
}

private fun codeSpanStyle(type: dev.wndavenz.notely.utils.CodeTokenType): SpanStyle {
    val color = when (type) {
        dev.wndavenz.notely.utils.CodeTokenType.KEYWORD -> CodeColors.Keyword
        dev.wndavenz.notely.utils.CodeTokenType.STRING -> CodeColors.String
        dev.wndavenz.notely.utils.CodeTokenType.NUMBER -> CodeColors.Number
        dev.wndavenz.notely.utils.CodeTokenType.COMMENT -> CodeColors.Comment
        dev.wndavenz.notely.utils.CodeTokenType.FUNCTION -> CodeColors.Function
        dev.wndavenz.notely.utils.CodeTokenType.TYPE -> CodeColors.Type
        dev.wndavenz.notely.utils.CodeTokenType.ANNOTATION -> CodeColors.Annotation
        else -> CodeColors.Plain
    }
    return SpanStyle(
        color = color,
        fontStyle = if (type == dev.wndavenz.notely.utils.CodeTokenType.COMMENT) {
            FontStyle.Italic
        } else {
            null
        },
    )
}

/**
 * Warnai blok kode menurut token sintaksnya, lalu pecah per baris supaya
 * tiap baris bisa diberi nomor. Token multi-baris (string/komentar blok)
 * tetap utuh warnanya karena pemecahan dilakukan sesudah pewarnaan.
 */
private fun highlightedCodeLines(
    code: String,
    language: String,
    query: String,
): List<AnnotatedString> {
    if (code.isEmpty()) return listOf(AnnotatedString(" "))
    val lines = ArrayList<AnnotatedString>()
    var builder = AnnotatedString.Builder()
    for (span in dev.wndavenz.notely.utils.CodeHighlight.highlight(code, language)) {
        val parts = span.text.split('\n')
        parts.forEachIndexed { index, part ->
            if (index > 0) {
                lines.add(builder.toAnnotatedString())
                builder = AnnotatedString.Builder()
            }
            if (part.isNotEmpty()) {
                builder.withStyle(codeSpanStyle(span.type)) { append(part) }
            }
        }
    }
    lines.add(builder.toAnnotatedString())
    return lines.map { line ->
        addHighlights(if (line.text.isEmpty()) AnnotatedString(" ") else line, query)
    }
}

/**
 * Satu baris bernomor: angka rata kanan, garis putih vertikal 1 dp sebagai
 * pembatas, lalu isi baris. Dipakai blok kode Markdown dan viewer TXT.
 * [content] menerima softWrap + modifier gulir horizontal (saat bungkus mati).
 */
@Composable
private fun NumberedLineRow(
    number: Int,
    compact: Boolean = false,
    wrap: Boolean = true,
    content: @Composable (softWrap: Boolean, modifier: Modifier) -> Unit,
) {
    val lineScroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
    ) {
        BasicText(
            text = number.toString(),
            style = (if (compact) NotelyText.Caption else NotelyText.Footnote).copy(
                color = NotelyColors.TertiaryLabel,
                textAlign = TextAlign.End,
                fontFamily = if (compact) FontFamily.Monospace else null,
            ),
            modifier = Modifier.width(if (compact) 26.dp else 30.dp),
        )
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .width(0.3.dp)
                .fillMaxHeight()
                .background(Color.White.copy(alpha = 0.7f)),
        )
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            content(
                wrap,
                if (wrap) Modifier else Modifier.horizontalScroll(lineScroll),
            )
        }
    }
}

/** Beri latar kuning gelap pada semua kemunculan [query] (abaikan huruf besar/kecil). */
private fun addHighlights(base: AnnotatedString, query: String): AnnotatedString {
    if (query.isBlank() || base.text.isEmpty()) return base
    val builder = AnnotatedString.Builder(base)
    var index = base.text.indexOf(query, 0, ignoreCase = true)
    while (index >= 0) {
        builder.addStyle(
            SpanStyle(background = Color(0xFF7A6200)),
            index,
            index + query.length,
        )
        index = base.text.indexOf(query, index + query.length, ignoreCase = true)
    }
    return builder.toAnnotatedString()
}

/** Render gaya sebaris Markdown (tebal, miring, kode, coret, tautan) + sorotan cari. */
private fun inlineMarkdown(text: String, query: String): AnnotatedString {
    val styled = buildAnnotatedString {
        for (span in MarkdownInline.parse(text)) {
            val style = SpanStyle(
                fontWeight = if (span.bold) FontWeight.Bold else null,
                fontStyle = if (span.italic) FontStyle.Italic else null,
                fontFamily = if (span.code) FontFamily.Monospace else null,
                textDecoration = when {
                    span.strikethrough -> TextDecoration.LineThrough
                    span.link -> TextDecoration.Underline
                    else -> null
                },
                color = when {
                    span.code -> CodeColors.Number
                    span.link -> CodeColors.Function
                    else -> Color.Unspecified
                },
            )
            withStyle(style) { append(span.text) }
        }
    }
    return addHighlights(styled, query)
}

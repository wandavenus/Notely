package dev.wndavenz.notely.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var showMenu by remember { mutableStateOf(false) }
    var showGoToLine by remember { mutableStateOf(false) }
    var showTextSizeSheet by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }

    // Ukuran teks viewer (pilihan menu titik tiga), tersimpan antar sesi.
    val context = LocalContext.current
    var textScale by remember { mutableStateOf(ReaderPrefs.textScale(context)) }

    val listState = rememberLazyListState()
    val editorScrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val blocksWithLines = remember(state.text, document.isMarkdown) {
        if (document.isMarkdown && state.loaded && !state.failed) {
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
    LaunchedEffect(query) {
        currentMatch = 0
        if (!editing && matchItems.isNotEmpty()) {
            listState.animateScrollToItem(matchItems[0])
        }
    }

    fun goToMatch(delta: Int) {
        if (matchItems.isEmpty()) return
        currentMatch = (currentMatch + delta).mod(matchItems.size)
        if (!editing) {
            scope.launch { listState.animateScrollToItem(matchItems[currentMatch]) }
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
                scope.launch { listState.animateScrollToItem(target) }
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

            document.isMarkdown -> LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp),
            ) {
                items(blocksWithLines.size) { index ->
                    MarkdownBlock(blocksWithLines[index].first, query, wordWrap, textScale)
                }
                if (state.truncated) {
                    item { TruncatedNote() }
                }
                item { Spacer(Modifier.height(32.dp)) }
            }

            else -> LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
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
                    showTextSizeSheet = true
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

    if (showTextSizeSheet) {
        val sizes = listOf(
            "Kecil" to 0.85f,
            "Sedang" to 1.0f,
            "Besar" to 1.15f,
            "Sangat Besar" to 1.3f,
        )
        IosActionSheet(
            actions = sizes.map { (label, scale) ->
                SheetAction(
                    label = if (textScale == scale) "$label ✓" else label,
                    color = NotelyColors.Label,
                ) {
                    textScale = scale
                    ReaderPrefs.setTextScale(context, scale)
                    showTextSizeSheet = false
                }
            },
            onDismiss = { showTextSizeSheet = false },
            title = "Ukuran Teks",
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
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .drawBehind {
                    // Garis vertikal pembatas nomor baris <-> teks (0,5 dp,
                    // putih 70% — lebih tipis & gelap dr putih penuh),
                    // setinggi isi editor yang sebenarnya (digambar belakangan
                    // supaya tidak perlu menebak tinggi konten).
                    val x = 12.dp.toPx() + 30.dp.toPx() + 8.dp.toPx()
                    drawLine(
                        color = Color.White.copy(alpha = 0.7f),
                        start = androidx.compose.ui.geometry.Offset(x, 0f),
                        end = androidx.compose.ui.geometry.Offset(x, size.height),
                        strokeWidth = 0.5.dp.toPx(),
                    )
                },
        ) {
            Column(Modifier.width(30.dp)) {
                for (number in 1..lineCount) {
                    BasicText(
                        text = number.toString(),
                        style = NotelyText.Caption.copy(
                            color = NotelyColors.TertiaryLabel,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.5.sp * textScale,
                            lineHeight = 20.sp * textScale,
                            textAlign = TextAlign.End,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
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

@Composable
private fun MarkdownBlock(block: MdBlock, query: String, wordWrap: Boolean, textScale: Float) {
    val bodyStyle = scaledTextStyle(NotelyText.EditorBody, textScale)
    when (block) {
        is MdBlock.Heading -> {
            Spacer(Modifier.height(if (block.level == 1) 10.dp else 16.dp))
            BasicText(
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
            )
            Spacer(Modifier.height(6.dp))
        }

        is MdBlock.Paragraph -> {
            BasicText(text = inlineMarkdown(block.text, query), style = bodyStyle)
            Spacer(Modifier.height(10.dp))
        }

        is MdBlock.Bullet -> {
            Row(Modifier.padding(start = 4.dp, bottom = 6.dp)) {
                BasicText(
                    text = when (block.checked) {
                        true -> "☑  "
                        false -> "☐  "
                        null -> "•  "
                    },
                    style = bodyStyle.copy(color = NotelyColors.Blue),
                )
                BasicText(
                    text = inlineMarkdown(block.text, query),
                    style = bodyStyle,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        is MdBlock.Numbered -> {
            Row(Modifier.padding(start = 4.dp, bottom = 6.dp)) {
                BasicText(
                    text = "${block.number}.  ",
                    style = bodyStyle.copy(color = NotelyColors.Blue),
                )
                BasicText(
                    text = inlineMarkdown(block.text, query),
                    style = bodyStyle,
                    modifier = Modifier.weight(1f),
                )
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
                codeLines.forEachIndexed { index, line ->
                    NumberedLineRow(number = index + 1, compact = true, wrap = wordWrap) { softWrap, modifier ->
                        BasicText(
                            text = line,
                            style = NotelyText.Callout.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.5.sp * textScale,
                                lineHeight = 19.sp * textScale,
                            ),
                            softWrap = softWrap,
                            modifier = modifier,
                        )
                    }
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
                BasicText(
                    text = inlineMarkdown(block.text, query),
                    style = bodyStyle.copy(color = NotelyColors.SecondaryLabel),
                    modifier = Modifier.weight(1f),
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
                .width(0.5.dp)
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

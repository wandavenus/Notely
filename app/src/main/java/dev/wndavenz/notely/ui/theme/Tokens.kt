package dev.wndavenz.notely.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.wndavenz.notely.R

/** Palet warna gaya iOS (mode gelap) untuk Notely. */
object NotelyColors {
    val Background = Color(0xFF000000)
    val Card = Color(0xFF1C1C1E)
    val CardPressed = Color(0xFF2C2C2E)
    val SearchFill = Color(0xFF2C2C2E)
    val Separator = Color(0xFF38383A)
    val Blue = Color(0xFF0A84FF)
    val Label = Color(0xFFFFFFFF)
    val SecondaryLabel = Color(0xFF98989F)
    val TertiaryLabel = Color(0xFF636366)
    val Destructive = Color(0xFFFF453A)
    val SheetBackground = Color(0xFF26262C)
    val SwitchOn = Color(0xFF34C759)
    val SwitchOff = Color(0xFF39393D)
}

/** Font Inter (dibundel sebagai resource font). */
val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

/** Token tipografi Inter untuk seluruh aplikasi. */
object NotelyText {
    val LargeTitle = TextStyle(
        color = NotelyColors.Label,
        fontFamily = InterFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 41.sp,
    )
    val NavTitle = TextStyle(
        color = NotelyColors.Label,
        fontFamily = InterFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    )
    val Body = TextStyle(
        color = NotelyColors.Label,
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 21.sp,
    )
    val BodySemibold = Body.copy(fontWeight = FontWeight.SemiBold)
    val Callout = TextStyle(
        color = NotelyColors.Label,
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    )
    val Footnote = TextStyle(
        color = NotelyColors.SecondaryLabel,
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )
    val Caption = TextStyle(
        color = NotelyColors.SecondaryLabel,
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )
    val SectionTitle = TextStyle(
        color = NotelyColors.Label,
        fontFamily = InterFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
    )
    val EditorTitle = TextStyle(
        color = NotelyColors.Label,
        fontFamily = InterFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 27.sp,
    )
    val EditorBody = TextStyle(
        color = NotelyColors.Label,
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.5.sp,
        lineHeight = 24.sp,
    )
}

/** Token bentuk: radius kartu layar Folder 9 dp, kartu catatan 12 dp. */
object NotelyShapes {
    val FolderCardRadius = 9.dp
    val NoteCardRadius = 12.dp
    val SheetRadius = 12.dp
    val SearchRadius = 9.dp
}

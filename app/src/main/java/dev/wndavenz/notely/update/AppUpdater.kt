package dev.wndavenz.notely.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Info rilis terbaru yang siap dipasang. */
data class UpdateInfo(
    val version: String,
    val downloadUrl: String,
)

/** Hasil pemeriksaan pembaruan. */
sealed interface CheckResult {
    data class Available(val info: UpdateInfo) : CheckResult
    data object UpToDate : CheckResult
    data object Failed : CheckResult
}

/**
 * Pembaruan mandiri lewat GitHub Releases repo Notely: periksa versi
 * terbaru, unduh APK-nya, lalu serahkan ke pemasang sistem. Tanda tangan
 * rilisnya tetap, jadi pemasangan menimpa versi lama tanpa hapus data.
 */
object AppUpdater {

    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/wandavenus/Notely/releases/latest"
    private const val PREFS_NAME = "pembaruan"
    private const val KEY_AUTO_CHECK = "cek_otomatis"

    /** Versi aplikasi yang sedang terpasang (dari PackageManager). */
    fun currentVersion(context: Context): String {
        val pm = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= 33) {
            pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, 0)
        }
        return info.versionName ?: "0"
    }

    /** Benar bila [latest] lebih baru dari [current] (format x.y.z). */
    fun isNewer(latest: String, current: String): Boolean {
        val l = latest.trim().removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val c = current.trim().removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(l.size, c.size)) {
            val lv = l.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (lv != cv) return lv > cv
        }
        return false
    }

    /** Periksa rilis terbaru di GitHub dan bandingkan dengan [currentVersion]. */
    suspend fun checkLatest(currentVersion: String): CheckResult =
        withContext(Dispatchers.IO) {
            try {
                val connection = URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.setRequestProperty("User-Agent", "Notely")
                connection.connectTimeout = 10_000
                connection.readTimeout = 15_000
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    return@withContext CheckResult.Failed
                }
                val json = JSONObject(
                    connection.inputStream.bufferedReader().use { it.readText() },
                )
                val latest = json.optString("tag_name").removePrefix("v")
                if (latest.isBlank() || !isNewer(latest, currentVersion)) {
                    return@withContext CheckResult.UpToDate
                }
                val assets = json.optJSONArray("assets") ?: return@withContext CheckResult.Failed
                var downloadUrl: String? = null
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    if (asset.optString("name").endsWith(".apk")) {
                        downloadUrl = asset.optString("browser_download_url")
                        break
                    }
                }
                val url = downloadUrl ?: return@withContext CheckResult.Failed
                CheckResult.Available(UpdateInfo(version = latest, downloadUrl = url))
            } catch (e: IOException) {
                CheckResult.Failed
            } catch (e: org.json.JSONException) {
                CheckResult.Failed
            }
        }

    /**
     * Unduh APK pembaruan ke cache. [onProgress] menerima 0..1, atau null
     * bila ukuran total tidak diketahui.
     */
    suspend fun download(
        context: Context,
        info: UpdateInfo,
        onProgress: (Float?) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val file = File(dir, "notely-${info.version}.apk")
        val connection = URL(info.downloadUrl).openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", "Notely")
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.connect()
        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            throw IOException("HTTP ${connection.responseCode}")
        }
        val total = connection.contentLengthLong
        connection.inputStream.use { input ->
            file.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024)
                var downloaded = 0L
                var count: Int
                while (input.read(buffer).also { count = it } >= 0) {
                    output.write(buffer, 0, count)
                    downloaded += count
                    onProgress(if (total > 0) downloaded.toFloat() / total else null)
                }
            }
        }
        file
    }

    /** Benar bila aplikasi sudah diizinkan memasang APK dari sumber tak dikenal. */
    fun canInstall(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    /** Intent pemasang sistem untuk [file] APK yang sudah diunduh. */
    fun installIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /** Saklar periksa pembaruan otomatis (bawaan: nyala). */
    fun autoCheckEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_CHECK, true)

    fun setAutoCheckEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_CHECK, enabled)
            .apply()
    }

    /** Intent ke pengaturan izin pasang dari sumber tak dikenal untuk aplikasi ini. */
    fun unknownSourcesIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

package com.smart222tech.pasha18tv

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class AppUpdate(
    val versionName: String,
    val versionCode: Long,
    val apkUrl: String,
    val sha256: String,
    val releaseNotes: String
)

object AppUpdateManager {
    private const val RELEASE_API =
        "https://api.github.com/repos/smart222tech/PASHXXX/releases/latest"

    suspend fun check(context: Context): AppUpdate? = withContext(Dispatchers.IO) {
        val connection = (URL(RELEASE_API).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "PASHXXX-Android-Updater")
        }
        try {
            if (connection.responseCode !in 200..299) return@withContext null
            val release = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            if (release.optBoolean("draft") || release.optBoolean("prerelease")) return@withContext null
            val assets = release.getJSONArray("assets")
            var apkUrl: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.getString("name")
                if (name.startsWith("PASHXXX-") && name.endsWith(".apk")) {
                    apkUrl = asset.getString("browser_download_url")
                    break
                }
            }
            val versionName = release.optString("tag_name").removePrefix("v")
            val versionCode = versionName.split(".").mapNotNull { it.toLongOrNull() }
                .fold(0L) { acc, part -> acc * 1000 + part }
            val current = if (Build.VERSION.SDK_INT >= 28) {
                context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toLong()
            }
            val notes = release.optString("body", "")
            val hash = Regex("(?im)^SHA256\\s*[:=]\\s*([a-f0-9]{64})\\s*$")
                .find(notes)?.groupValues?.get(1) ?: return@withContext null
            if (apkUrl == null || versionCode <= current) return@withContext null
            AppUpdate(versionName, versionCode, apkUrl, hash, notes)
        } finally {
            connection.disconnect()
        }
    }

    suspend fun downloadAndInstall(
        context: Context,
        update: AppUpdate,
        onProgress: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        // Request the Android install permission before downloading a potentially large APK.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            withContext(Dispatchers.Main) {
                val settings = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(settings)
            }
            return@withContext false
        }

        val directory = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "updates")
        if (!directory.exists()) directory.mkdirs()
        val apk = File(directory, "PASHXXX-${update.versionName}.apk")
        apk.delete()
        val request = DownloadManager.Request(Uri.parse(update.apkUrl))
            .setTitle("PASHXXX ${update.versionName}")
            .setDescription("Загрузка обновления")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationUri(Uri.fromFile(apk))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val id = manager.enqueue(request)
        var finished = false
        try {
            while (!finished) {
                val cursor = manager.query(DownloadManager.Query().setFilterById(id))
                cursor.use {
                    if (it != null && it.moveToFirst()) {
                        val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        val total = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        val soFar = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        if (total > 0) {
                            val progress = ((soFar * 100) / total).toInt().coerceIn(0, 100)
                            withContext(Dispatchers.Main) { onProgress(progress) }
                        }
                        when (status) {
                            DownloadManager.STATUS_SUCCESSFUL -> finished = true
                            DownloadManager.STATUS_FAILED -> {
                                val reason = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                                error("GitHub APK download failed (reason $reason)")
                            }
                        }
                    } else error("Download task disappeared")
                }
                if (!finished) delay(500)
            }
        } catch (failure: Throwable) {
            manager.remove(id)
            apk.delete()
            throw failure
        }

        // Stream the APK through SHA-256 instead of loading the entire file into RAM.
        val digest = MessageDigest.getInstance("SHA-256")
        apk.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        val actualHash = digest.digest().joinToString("") { byte ->
            "%02x".format(byte.toInt() and 0xff)
        }
        if (!actualHash.equals(update.sha256, ignoreCase = true)) {
            apk.delete()
            error("SHA-256 verification failed; APK was not installed")
        }

        withContext(Dispatchers.Main) {
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", apk
            )
            val install = Intent(Intent.ACTION_INSTALL_PACKAGE, uri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(install)
        }
        true
    }
}

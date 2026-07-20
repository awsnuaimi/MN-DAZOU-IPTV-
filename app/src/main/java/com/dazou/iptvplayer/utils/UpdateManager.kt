package com.dazou.iptvplayer.utils

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import com.dazou.iptvplayer.R
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/**
 * يتحقق من وجود إصدار أحدث على GitHub Releases، ويعرض حوار تحديث،
 * ويحمّل ويثبّت الإصدار الجديد مباشرة من داخل التطبيق.
 */
object UpdateManager {

    // ⚠️ تأكد إنه هذا بالضبط owner/repo الخاص فيك على GitHub
    private const val REPO = "awsnuaimi/MN-DAZOU-IPTV-"
    private const val API_URL = "https://api.github.com/repos/$REPO/releases/latest"

    private var isBusy = false

    private data class UpdateInfo(val tagName: String, val apkUrl: String, val notes: String)

    /** silent = true: ما يطلع أي رسالة لو ما في تحديث (مناسب للفحص التلقائي عند فتح التطبيق) */
    fun checkForUpdate(context: Context, silent: Boolean) {
        if (isBusy) return
        thread {
            try {
                val conn = URL(API_URL).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/vnd.github+json")
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                val text = conn.inputStream.bufferedReader().readText()
                conn.disconnect()

                val json = JSONObject(text)
                val tag = json.optString("tag_name", "")
                val notes = json.optString("body", "")
                val assets = json.optJSONArray("assets") ?: JSONArray()

                var apkUrl: String? = null
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    if (asset.optString("name", "").endsWith(".apk")) {
                        apkUrl = asset.optString("browser_download_url")
                        break
                    }
                }

                val currentVersion = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"
                } catch (e: Exception) { "0" }

                if (tag.isNotEmpty() && apkUrl != null && isNewerVersion(tag, currentVersion)) {
                    val info = UpdateInfo(tag, apkUrl, notes)
                    runOnUiThread { showUpdateDialog(context, info) }
                } else if (!silent) {
                    runOnUiThread {
                        Toast.makeText(context, context.getString(R.string.update_no_update), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                if (!silent) {
                    runOnUiThread {
                        Toast.makeText(context, context.getString(R.string.update_check_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun isNewerVersion(remoteTag: String, currentVersionName: String): Boolean {
        val remote = remoteTag.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        val current = currentVersionName.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(remote.size, current.size)
        for (i in 0 until maxLen) {
            val r = remote.getOrElse(i) { 0 }
            val c = current.getOrElse(i) { 0 }
            if (r != c) return r > c
        }
        return false
    }

    private fun showUpdateDialog(context: Context, info: UpdateInfo) {
        val message = context.getString(R.string.update_available_message, info.tagName) +
            if (info.notes.isNotBlank()) "\n\n${info.notes.take(300)}" else ""

        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.update_available_title))
            .setMessage(message)
            .setPositiveButton(context.getString(R.string.update_now)) { _, _ ->
                startDownloadAndInstall(context, info.apkUrl)
            }
            .setNegativeButton(context.getString(R.string.update_later), null)
            .setCancelable(true)
            .show()
    }

    private fun startDownloadAndInstall(context: Context, apkUrl: String) {
        if (isBusy) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            Toast.makeText(context, context.getString(R.string.update_permission_needed), Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            context.startActivity(intent)
            return
        }

        isBusy = true
        val progressDialog = AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.update_downloading))
            .setCancelable(false)
            .create()
        progressDialog.show()

        thread {
            try {
                val conn = URL(apkUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.instanceFollowRedirects = true
                conn.connect()

                val dir = File(context.getExternalFilesDir(null), "updates")
                if (!dir.exists()) dir.mkdirs()
                val apkFile = File(dir, "update.apk")

                conn.inputStream.use { input ->
                    FileOutputStream(apkFile).use { output ->
                        input.copyTo(output)
                    }
                }
                conn.disconnect()

                runOnUiThread {
                    progressDialog.dismiss()
                    isBusy = false
                    installApk(context, apkFile)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progressDialog.dismiss()
                    isBusy = false
                    Toast.makeText(context, context.getString(R.string.update_download_failed), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    private fun runOnUiThread(action: () -> Unit) {
        Handler(Looper.getMainLooper()).post(action)
    }
}
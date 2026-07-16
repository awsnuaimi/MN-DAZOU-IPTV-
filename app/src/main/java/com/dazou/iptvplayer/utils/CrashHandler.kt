package com.dazou.iptvplayer.utils

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val defaultHandler: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            saveCrashLog(throwable)
        } catch (e: Exception) {
            // تجاهل أي خطأ أثناء الحفظ نفسه
        }
        defaultHandler?.uncaughtException(thread, throwable)
    }

    private fun saveCrashLog(throwable: Throwable) {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        val stackTraceText = sw.toString()

        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        val fileName = "crash_$timestamp.txt"

        val dir = File(context.getExternalFilesDir(null), "crash_logs")
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, fileName)
        file.writeText(
            "===== CRASH REPORT =====\n" +
            "Time: $timestamp\n" +
            "Message: ${throwable.message}\n\n" +
            "Stack Trace:\n$stackTraceText"
        )
    }

    companion object {
        fun install(context: Context) {
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(context.applicationContext))
        }
    }
}
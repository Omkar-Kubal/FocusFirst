package com.focusfirst.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.focusfirst.data.db.SessionDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionDao: SessionDao
) {
    suspend fun exportSessionsToCsv() {
        withContext(Dispatchers.IO) {
            val sessions = sessionDao.observeAll().first()
            val csvBuilder = StringBuilder()
            csvBuilder.append("ID,StartedAt,DurationSeconds,WasCompleted,Tag\n")
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            sessions.forEach { session ->
                val dateString = dateFormat.format(Date(session.startedAt))
                csvBuilder.append("${session.id},\"${dateString}\",${session.durationSeconds},${session.wasCompleted},\"${session.tag}\"\n")
            }

            val fileName = "FocusFirst_Export_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            file.writeText(csvBuilder.toString())

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            val chooserIntent = Intent.createChooser(shareIntent, "Export Focus Data")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)
        }
    }
}

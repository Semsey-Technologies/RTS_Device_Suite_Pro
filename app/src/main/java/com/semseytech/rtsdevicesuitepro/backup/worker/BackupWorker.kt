package com.semseytech.rtsdevicesuitepro.backup.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.semseytech.rtsdevicesuitepro.MainActivity
import com.semseytech.rtsdevicesuitepro.archive.model.ArchiveFormat
import com.semseytech.rtsdevicesuitepro.backup.engine.BackupEngine
import com.semseytech.rtsdevicesuitepro.backup.model.BackupDestination
import com.semseytech.rtsdevicesuitepro.backup.model.BackupItem
import java.io.File

class BackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val CHANNEL_ID = "backup_channel"
    private val NOTIFICATION_ID = 1001

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo(0f, "Preparing backup...")
    }

    override suspend fun doWork(): Result {
        val inputFilePath = inputData.getString("input_file_path") ?: return Result.failure()
        val outputFileName = inputData.getString("output_file_name") ?: "RTS_Backup"
        val archiveFormatName = inputData.getString("archive_format") ?: ArchiveFormat.ZIP.name
        val archiveFormat = ArchiveFormat.valueOf(archiveFormatName)
        val compressionLevel = inputData.getInt("compression_level", 5)
        val destinationJson = inputData.getString("destination_json") ?: return Result.failure()
        
        val gson = Gson()
        val destination = gson.fromJson(destinationJson, BackupDestination::class.java)

        createNotificationChannel()
        setForeground(createForegroundInfo(0f, "Preparing backup..."))

        try {
            val inputFile = File(inputFilePath)
            if (!inputFile.exists()) return Result.failure()
            
            val jsonContent = inputFile.readText()
            // We need a custom deserializer for BackupItem because it's a sealed class
            // For now, let's assume we use a specialized approach or Gson with RuntimeTypeAdapterFactory if available.
            // Since I don't have RuntimeTypeAdapterFactory, I'll use a simpler trick:
            // The UI will save the selected items to a temporary file using a format that includes type info.
            
            // Re-evaluating: BackupEngine needs List<BackupItem>.
            // I'll check how BackupViewModel serializes it.
            
            val items: List<BackupItem> = deserializeBackupItems(jsonContent)
            
            val engine = BackupEngine(applicationContext as android.app.Application)
            val resultFile = engine.runBackup(
                selectedItems = items,
                archiveFormat = archiveFormat,
                compressionLevel = compressionLevel,
                outputFileName = outputFileName,
                destination = destination
            ) { progress, status ->
                // Update notification directly without setForeground if possible, 
                // or use a non-suspending way to trigger it.
                showProgressNotification(progress, status)
            }

            inputFile.delete()

            if (resultFile != null) {
                showFinalNotification("Backup Complete", "Successfully saved to ${resultFile.name}")
                return Result.success()
            } else {
                showFinalNotification("Backup Failed", "An error occurred during the backup process.")
                return Result.failure()
            }

        } catch (e: Exception) {
            Log.e("BackupWorker", "Error in BackupWorker", e)
            showFinalNotification("Backup Error", e.message ?: "Unknown error")
            return Result.failure()
        }
    }

    private fun deserializeBackupItems(json: String): List<BackupItem> {
        // Simple manual deserialization for the sealed class if necessary, 
        // but let's try if Gson can handle it with the @type field if we added it,
        // or just use a generic list and post-process.
        // Actually, if we use a library like RuntimeTypeAdapterFactory it's easier.
        // Without it, I'll use a custom approach in the ViewModel to save type info.
        
        val gson = Gson()
        val listType = object : TypeToken<List<Map<String, Any>>>() {}.type
        val rawList: List<Map<String, Any>> = gson.fromJson(json, listType)
        
        return rawList.map { map ->
            val type = map["itemClassType"] as String
            val itemJson = gson.toJson(map)
            when (type) {
                "SmsMessage" -> gson.fromJson(itemJson, BackupItem.SmsMessage::class.java)
                "CallLogEntry" -> gson.fromJson(itemJson, BackupItem.CallLogEntry::class.java)
                "Contact" -> gson.fromJson(itemJson, BackupItem.Contact::class.java)
                "Apk" -> gson.fromJson(itemJson, BackupItem.Apk::class.java)
                "UserFile" -> gson.fromJson(itemJson, BackupItem.UserFile::class.java)
                "SystemSetting" -> gson.fromJson(itemJson, BackupItem.SystemSetting::class.java)
                else -> throw IllegalArgumentException("Unknown type: $type")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Backup Service",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundInfo(progress: Float, status: String): ForegroundInfo {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Backing up device...")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, (progress * 100).toInt(), false)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun showProgressNotification(progress: Float, status: String) {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Backing up device...")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, (progress * 100).toInt(), false)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showFinalNotification(title: String, message: String) {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }
}

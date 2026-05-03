package com.semseytech.rtsdevicesuitepro.backup.engine

import android.app.Application
import android.app.WallpaperManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Settings
import android.provider.Telephony
import com.semseytech.rtsdevicesuitepro.backup.model.BackupItem
import java.io.File

class BackupScanner(private val application: Application) {

    fun scanSmsThreads(): List<BackupItem.SmsMessage> {
        val threads = mutableMapOf<String, MutableList<BackupItem.MessageDetail>>()
        val context = application.applicationContext
        
        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.THREAD_ID),
            null, null, Telephony.Sms.DATE + " DESC"
        )

        cursor?.use {
            val idIdx = it.getColumnIndex(Telephony.Sms._ID)
            val addrIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
            val dateIdx = it.getColumnIndex(Telephony.Sms.DATE)
            val tidIdx = it.getColumnIndex(Telephony.Sms.THREAD_ID)

            while (it.moveToNext()) {
                val tid = it.getString(tidIdx) ?: "0"
                if (threads.size > 100) break 
                val address = it.getString(addrIdx) ?: "Unknown"
                val detail = BackupItem.MessageDetail(
                    id = it.getString(idIdx) ?: it.position.toString(),
                    body = it.getString(bodyIdx) ?: "",
                    date = it.getLong(dateIdx),
                    dateSent = 0, type = 1
                )
                threads.getOrPut(tid) { mutableListOf() }.add(detail)
            }
        }

        return threads.map { (tid, msgs) ->
            val latest = msgs.first()
            BackupItem.SmsMessage(tid, latest.body, latest.body, latest.date, msgs.size)
        }
    }

    fun scanCallLogs(): List<BackupItem.CallLogEntry> {
        val list = mutableListOf<BackupItem.CallLogEntry>()
        val cursor = application.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE, CallLog.Calls.DURATION, CallLog.Calls._ID),
            null, null, CallLog.Calls.DATE + " DESC"
        )
        cursor?.use {
            val numIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
            val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)
            val typeIdx = it.getColumnIndex(CallLog.Calls.TYPE)
            val durIdx = it.getColumnIndex(CallLog.Calls.DURATION)
            val idIdx = it.getColumnIndex(CallLog.Calls._ID)
            
            while (it.moveToNext() && it.position < 500) {
                val num = it.getString(numIdx) ?: "Unknown"
                val date = it.getLong(dateIdx)
                val dur = it.getLong(durIdx)
                val type = it.getInt(typeIdx)
                val id = it.getString(idIdx) ?: it.position.toString()
                
                list.add(BackupItem.CallLogEntry(
                    id = id,
                    number = num,
                    latestType = type.toString(),
                    date = date,
                    totalDuration = dur,
                    size = 100L,
                    calls = listOf(BackupItem.CallDetail(id, type, date, dur))
                ))
            }
        }
        return list
    }

    fun scanContacts(): List<BackupItem.Contact> {
        val list = mutableListOf<BackupItem.Contact>()
        val contentResolver = application.contentResolver
        val cursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP, ContactsContract.Contacts.PHOTO_URI),
            null, null, ContactsContract.Contacts.DISPLAY_NAME + " ASC"
        )
        cursor?.use {
            val idIdx = it.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIdx = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val dateIdx = it.getColumnIndex(ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP)
            val photoIdx = it.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)
            
            while (it.moveToNext() && it.position < 1000) {
                val id = it.getString(idIdx)
                val name = it.getString(nameIdx) ?: "Unknown"
                val date = if (dateIdx != -1) it.getLong(dateIdx) else 0L
                val photoUri = if (photoIdx != -1) it.getString(photoIdx) else null
                
                val phoneNumbers = mutableListOf<String>()
                val emails = mutableListOf<String>()
                
                // Fetch Phone Numbers
                val phoneCursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(id), null
                )
                phoneCursor?.use { pc ->
                    val numIdx = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    while (pc.moveToNext()) {
                        pc.getString(numIdx)?.let { num -> phoneNumbers.add(num) }
                    }
                }
                
                // Fetch Emails
                val emailCursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
                    "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                    arrayOf(id), null
                )
                emailCursor?.use { ec ->
                    val addrIdx = ec.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                    while (ec.moveToNext()) {
                        ec.getString(addrIdx)?.let { addr -> emails.add(addr) }
                    }
                }
                
                list.add(BackupItem.Contact(
                    id = id, 
                    name = name, 
                    phoneNumbers = phoneNumbers, 
                    emails = emails, 
                    date = date, 
                    size = 150L,
                    photoUri = photoUri
                ))
            }
        }
        return list
    }

    fun scanApks(): List<BackupItem.Apk> {
        val pm = application.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .map {
                val info = pm.getPackageInfo(it.packageName, 0)
                BackupItem.Apk(
                    it.packageName,
                    it.loadLabel(pm).toString(),
                    it.packageName,
                    info.versionName ?: "1.0",
                    it.sourceDir,
                    size = File(it.sourceDir).length()
                )
            }
    }

    fun scanUserFiles(category: String): List<BackupItem.UserFile> {
        val list = mutableListOf<BackupItem.UserFile>()
        val dir = when (category) {
            "Pictures" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            "Videos" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            "Audio" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            "Music" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            "Movies" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            "Documents" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            "Downloads" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            "Ringtones" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES)
            "Notifications" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_NOTIFICATIONS)
            "Alarms" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_ALARMS)
            else -> Environment.getExternalStorageDirectory()
        }
        
        dir.walkTopDown().maxDepth(3).forEach { file ->
            if (file.isFile && !file.name.startsWith(".")) {
                list.add(BackupItem.UserFile(
                    file.absolutePath, file.name, file.length(), file.absolutePath,
                    "application/octet-stream", file.lastModified(), category
                ))
            }
        }
        return list
    }

    fun scanSystemSettings(): List<BackupItem.SystemSetting> {
        val settings = mutableListOf<BackupItem.SystemSetting>()
        
        // WiFi (Limited on non-root)
        val wifiManager = application.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            wifiManager.configuredNetworks?.forEach {
                settings.add(BackupItem.SystemSetting(it.SSID, it.SSID, "SSID", "WiFi"))
            }
        } else {
            // Suggestion: Scan for saved networks if possible or just list known SSIDs
            settings.add(BackupItem.SystemSetting("wifi_disclaimer", "WiFi Backup", "Limited by Android version", "WiFi"))
        }

        // Bluetooth
        val bluetoothManager = application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        bluetoothAdapter?.bondedDevices?.forEach {
            settings.add(BackupItem.SystemSetting(it.address, it.name ?: "Unknown", it.address, "Bluetooth"))
        }

        // Wallpaper info
        val wallpaperManager = WallpaperManager.getInstance(application)
        settings.add(BackupItem.SystemSetting("wallpaper", "Current Wallpaper", "Image Data", "Wallpaper"))

        // Accessibility Settings
        val accessibilityEnabled = Settings.Secure.getInt(application.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0)
        settings.add(BackupItem.SystemSetting("accessibility", "Accessibility Enabled", accessibilityEnabled.toString(), "Settings"))

        return settings
    }
}

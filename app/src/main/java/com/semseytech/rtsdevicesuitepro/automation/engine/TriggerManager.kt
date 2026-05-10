package com.semseytech.rtsdevicesuitepro.automation.engine

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.os.FileObserver
import java.io.File
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.PowerManager
import android.os.Build
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.media.AudioRecordingConfiguration
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import kotlinx.coroutines.*
import java.net.InetAddress
import java.net.NetworkInterface

class TriggerManager(private val context: Context, private val engine: AutomationEngine) : SensorEventListener {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    
    private var lastIpAddress: String? = null
    private var lastCallState: String? = null
    private var periodicJob: kotlinx.coroutines.Job? = null
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
    private var mediaRecorder: android.media.MediaRecorder? = null
    private val fileObservers = mutableMapOf<String, FileObserver>()
    private var screenshotObserver: android.database.ContentObserver? = null
    private val activeSensors = mutableSetOf<Int>()
    private var isLocationUpdatesStarted = false

    // Sensor values
    private var lastStepCount = -1
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    
    // Throttling values
    private var lastHeading = -1
    private var lastMagneticStrength = -1
    private var lastPressure = -1
    private var lastAltitude = -1
    private var lastLight = -1
    private var lastTemp = -1
    private var lastHumidity = -1
    private var lastHeartRate = -1
    private var lastActivity: String? = null
    private var lastPattern: String? = null

    private val monitoredHosts = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private val monitoredDomains = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true) {
                engine.onTrigger("VPN_CONNECTED")
            }
            engine.onTrigger("NETWORK_AVAILABLE")
            checkIpAddressChange()
        }

        override fun onLost(network: Network) {
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true) {
                engine.onTrigger("MOBILE_DATA_OFF")
            }
            
            val activeNetworks = connectivityManager.allNetworks
            val hasVpn = activeNetworks.any { 
                connectivityManager.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true 
            }
            if (!hasVpn) {
                engine.onTrigger("VPN_DISCONNECTED")
            }
            engine.onTrigger("NETWORK_LOST")
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                engine.onTrigger("MOBILE_DATA_ON")
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                val type = try {
                    @Suppress("DEPRECATION")
                    when (telephonyManager.networkType) {
                        TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_EDGE -> "2G"
                        TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_HSDPA -> "3G"
                        TelephonyManager.NETWORK_TYPE_LTE -> "4G"
                        TelephonyManager.NETWORK_TYPE_NR -> "5G"
                        else -> "ANY"
                    }
                } catch (e: SecurityException) {
                    "ANY"
                }
                engine.onTrigger("MOBILE_DATA_TYPE_CHANGED", type)
            }
        }
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val speedKmh = (location.speed * 3.6).toInt()
            engine.onTrigger("SPEED_THRESHOLD", speedKmh)
            engine.onTrigger("SIGNIFICANT_LOCATION_CHANGE", location)
            engine.onTrigger("LOCATION_UPDATE", location)
            engine.onTrigger("GEOFENCE_ENTER", location)
            engine.onTrigger("GEOFENCE_EXIT", location)
            engine.onTrigger("ARRIVE_AT_PLACE", location)
            engine.onTrigger("LEAVE_PLACE", location)
            
            if (location.hasAltitude()) {
                engine.onTrigger("ALTITUDE_THRESHOLD", location.altitude.toInt())
            }
        }
        override fun onProviderEnabled(provider: String) {
            if (provider == LocationManager.GPS_PROVIDER) engine.onTrigger("GPS_STATE_CHANGED", true)
        }
        override fun onProviderDisabled(provider: String) {
            if (provider == LocationManager.GPS_PROVIDER) engine.onTrigger("GPS_STATE_CHANGED", false)
        }
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
    }

    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        engine.onTrigger("CLIPBOARD_CHANGED")
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

    private val playbackCallback = @SuppressLint("NewApi") object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>) {
            // The list contains the playback configurations of all players that are currently active.
            val isPlaying = configs.isNotEmpty()
            if (isPlaying) engine.onTrigger("MUSIC_STATE_CHANGED", "PLAYING")
            
            configs.forEach { config ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        // getClientUid is @SystemApi, we use reflection to access it if needed
                        val getClientUid = config.javaClass.getMethod("getClientUid")
                        val uid = getClientUid.invoke(config) as Int
                        val packageName = context.packageManager.getPackagesForUid(uid)?.firstOrNull()
                        if (packageName != null) engine.onTrigger("APP_PLAYING_AUDIO", packageName)
                    } catch (e: Exception) {
                        // If hidden API is inaccessible, we can't get the package name
                    }
                }
            }
        }
    }

    private val recordingCallback = @SuppressLint("NewApi") object : AudioManager.AudioRecordingCallback() {
        override fun onRecordingConfigChanged(configs: List<AudioRecordingConfiguration>) {
            if (configs.isNotEmpty()) engine.onTrigger("MICROPHONE_ACTIVATED")
        }
    }

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        controllers?.forEach { controller ->
            controller.registerCallback(object : MediaController.Callback() {
                override fun onMetadataChanged(metadata: MediaMetadata?) {
                    engine.onTrigger("MEDIA_METADATA_CHANGED")
                }
                override fun onPlaybackStateChanged(state: PlaybackState?) {
                    val s = when (state?.state) {
                        PlaybackState.STATE_PLAYING -> "PLAYING"
                        PlaybackState.STATE_PAUSED -> "PAUSED"
                        PlaybackState.STATE_STOPPED -> "STOPPED"
                        else -> null
                    }
                    if (s != null) engine.onTrigger("MUSIC_STATE_CHANGED", s)
                }
            })
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("TriggerManager", "onReceive: ${intent.action}")
            when (intent.action) {
                WifiManager.NETWORK_STATE_CHANGED_ACTION -> {
                    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    val info = try { wifiManager.connectionInfo } catch (e: SecurityException) { null }
                    val ssid = info?.ssid?.replace("\"", "")
                    if (ssid != null && ssid != "<unknown ssid>") {
                        engine.onTrigger("SPECIFIC_WIFI_CONNECTED", ssid)
                        engine.onTrigger("WIFI_CONNECTED", ssid)
                    } else if (info?.networkId == -1) {
                        engine.onTrigger("WIFI_DISCONNECTED")
                    } else {
                        engine.onTrigger("WIFI_CONNECTED")
                    }
                }
                WifiManager.RSSI_CHANGED_ACTION -> {
                    val rssi = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, -100)
                    engine.onTrigger("WIFI_SIGNAL_THRESHOLD", rssi)
                }
                "android.net.wifi.WIFI_AP_STATE_CHANGED" -> {
                    val state = intent.getIntExtra("wifi_state", 11)
                    if (state == 13) engine.onTrigger("HOTSPOT_ON")
                    else if (state == 11) engine.onTrigger("HOTSPOT_OFF")
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val pct = (level * 100 / scale.toFloat()).toInt()
                    engine.onTrigger("BATTERY_LEVEL_ABOVE", pct)
                    engine.onTrigger("BATTERY_LEVEL_BELOW", pct)
                    val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10
                    engine.onTrigger("BATTERY_TEMP_ABOVE", temp)
                    engine.onTrigger("BATTERY_TEMP_BELOW", temp)
                    if (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) == BatteryManager.BATTERY_PLUGGED_WIRELESS) {
                        engine.onTrigger("BATTERY_WIRELESS_CHARGING")
                    }
                }
                Intent.ACTION_SCREEN_ON -> engine.onTrigger("SCREEN_ON")
                Intent.ACTION_SCREEN_OFF -> {
                    engine.onTrigger("SCREEN_OFF")
                    engine.onTrigger("SCREEN_LOCKED")
                }
                Intent.ACTION_POWER_CONNECTED -> {
                    engine.onTrigger("POWER_CONNECTED")
                    val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    val chargePlug = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
                    if (chargePlug == BatteryManager.BATTERY_PLUGGED_USB) {
                        engine.onTrigger("USB_CONNECTED")
                    }
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    engine.onTrigger("POWER_DISCONNECTED")
                    engine.onTrigger("USB_DISCONNECTED")
                }
                Intent.ACTION_USER_PRESENT -> engine.onTrigger("SCREEN_UNLOCKED")
                Intent.ACTION_BOOT_COMPLETED -> engine.onTrigger("BOOT_COMPLETED")
                Intent.ACTION_SHUTDOWN -> engine.onTrigger("SHUTDOWN_INITIATED")
                Intent.ACTION_REBOOT -> engine.onTrigger("REBOOT_REQUESTED")
                PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED -> {
                    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                    if (pm.isDeviceIdleMode) {
                        engine.onTrigger("DOZE_MODE_ENTERED")
                        engine.onTrigger("DEVICE_IDLE_ENTERED")
                    } else {
                        engine.onTrigger("DOZE_MODE_EXITED")
                    }
                }
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                    if (pm.isPowerSaveMode) engine.onTrigger("POWER_SAVE_ON")
                    else engine.onTrigger("POWER_SAVE_OFF")
                }
                Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                    val state = intent.getBooleanExtra("state", false)
                    if (state) engine.onTrigger("AIRPLANE_MODE_ON")
                    else engine.onTrigger("AIRPLANE_MODE_OFF")
                }
                Intent.ACTION_HEADSET_PLUG -> {
                    val state = intent.getIntExtra("state", -1)
                    if (state == 1) engine.onTrigger("HEADPHONES_PLUGGED")
                    else if (state == 0) engine.onTrigger("HEADPHONES_UNPLUGGED")
                }
                Intent.ACTION_MEDIA_MOUNTED -> engine.onTrigger("STORAGE_MOUNTED")
                Intent.ACTION_MEDIA_UNMOUNTED, Intent.ACTION_MEDIA_EJECT -> engine.onTrigger("STORAGE_UNMOUNTED")
                android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE -> engine.onTrigger("DOWNLOAD_COMPLETED")
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    if (state == BluetoothAdapter.STATE_ON) engine.onTrigger("BLUETOOTH_ON")
                    else if (state == BluetoothAdapter.STATE_OFF) engine.onTrigger("BLUETOOTH_OFF")
                }
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    engine.onTrigger("BT_DEVICE_CONNECTED", device?.address)
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    engine.onTrigger("BT_DEVICE_DISCONNECTED", device?.address)
                }
                Intent.ACTION_CONFIGURATION_CHANGED -> engine.onTrigger("ORIENTATION_CHANGED")
                LocationManager.PROVIDERS_CHANGED_ACTION -> {
                    val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    engine.onTrigger("GPS_STATE_CHANGED", isGpsEnabled)
                }
                AudioManager.RINGER_MODE_CHANGED_ACTION -> {
                    val mode = when (intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE, -1)) {
                        AudioManager.RINGER_MODE_NORMAL -> "NORMAL"
                        AudioManager.RINGER_MODE_VIBRATE -> "VIBRATE"
                        AudioManager.RINGER_MODE_SILENT -> "SILENT"
                        else -> null
                    }
                    if (mode != null) engine.onTrigger("RINGER_MODE_CHANGED", mode)
                }
                "android.media.VOLUME_CHANGED_ACTION" -> {
                    val streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
                    val value = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", -1)
                    val streamName = when (streamType) {
                        AudioManager.STREAM_MUSIC -> "MUSIC"
                        AudioManager.STREAM_RING -> "RINGER"
                        AudioManager.STREAM_ALARM -> "ALARM"
                        AudioManager.STREAM_NOTIFICATION -> "NOTIFICATION"
                        else -> "OTHER"
                    }
                    val max = audioManager.getStreamMaxVolume(streamType)
                    val pct = if (max > 0) (value * 100 / max) else 0
                    engine.onTrigger("VOLUME_CHANGED", Pair(streamName, pct))
                }
                AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                    engine.onTrigger("AUDIO_DEVICE_CONNECTED", "SPEAKER")
                }
                Intent.ACTION_PACKAGE_ADDED -> {
                    val packageName = intent.data?.schemeSpecificPart
                    if (packageName != null) engine.onTrigger("APP_INSTALLED", packageName)
                }
                Intent.ACTION_PACKAGE_REMOVED -> {
                    val packageName = intent.data?.schemeSpecificPart
                    if (packageName != null && !intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                        engine.onTrigger("APP_UNINSTALLED", packageName)
                    }
                }
                Intent.ACTION_PACKAGE_REPLACED -> {
                    val packageName = intent.data?.schemeSpecificPart
                    if (packageName != null) engine.onTrigger("APP_UPDATED", packageName)
                }
                "android.provider.Telephony.SMS_RECEIVED" -> {
                    engine.onTrigger("SMS_RECEIVED")
                    val pdus = intent.extras?.get("pdus") as? Array<*>
                    val format = intent.extras?.getString("format")
                    pdus?.forEach { pdu ->
                        val sms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            android.telephony.SmsMessage.createFromPdu(pdu as ByteArray, format)
                        } else {
                            @Suppress("DEPRECATION")
                            android.telephony.SmsMessage.createFromPdu(pdu as ByteArray)
                        }
                        sms.originatingAddress?.let { engine.onTrigger("SMS_FROM_CONTACT", it) }
                    }
                }
                "android.provider.Telephony.WAP_PUSH_RECEIVED" -> engine.onTrigger("MMS_RECEIVED")
                TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                    val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                    val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                    Log.d("TriggerManager", "Call state changed: $lastCallState -> $state")
                    
                    when (state) {
                        TelephonyManager.EXTRA_STATE_RINGING -> engine.onTrigger("CALL_INCOMING", number)
                        TelephonyManager.EXTRA_STATE_OFFHOOK -> engine.onTrigger("CALL_ANSWERED")
                        TelephonyManager.EXTRA_STATE_IDLE -> {
                            if (lastCallState == TelephonyManager.EXTRA_STATE_RINGING) {
                                engine.onTrigger("CALL_MISSED")
                            }
                            engine.onTrigger("CALL_ENDED")
                        }
                    }
                    lastCallState = state
                }
                else -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && intent.action == TelephonyManager.ACTION_SHOW_VOICEMAIL_NOTIFICATION) {
                        engine.onTrigger("VOICEMAIL_RECEIVED")
                    }
                }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val steps = event.values[0].toInt()
                if (lastStepCount != -1) engine.onTrigger("STEP_COUNT_THRESHOLD", steps)
                lastStepCount = steps
            }
            Sensor.TYPE_PROXIMITY -> engine.onTrigger("PROXIMITY_TRIGGERED", event.values[0] < event.sensor.maximumRange)
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometerReading, 0, 3)
                updateOrientation()
                detectMovementPattern(event.values)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetometerReading, 0, 3)
                updateOrientation()
            }
            Sensor.TYPE_GYROSCOPE -> detectRotationPattern(event.values)
            Sensor.TYPE_PRESSURE -> {
                val pressure = event.values[0].toInt()
                if (pressure != lastPressure) {
                    val altitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, event.values[0]).toInt()
                    if (altitude != lastAltitude) {
                        engine.onTrigger("ALTITUDE_THRESHOLD", altitude)
                        lastAltitude = altitude
                    }
                    engine.onTrigger("SENSOR_PRESSURE", pressure)
                    lastPressure = pressure
                }
            }
            Sensor.TYPE_AMBIENT_TEMPERATURE -> {
                val temp = event.values[0].toInt()
                if (temp != lastTemp) {
                    engine.onTrigger("SENSOR_TEMP", temp)
                    lastTemp = temp
                }
            }
            Sensor.TYPE_RELATIVE_HUMIDITY -> {
                val humidity = event.values[0].toInt()
                if (humidity != lastHumidity) {
                    engine.onTrigger("SENSOR_HUMIDITY", humidity)
                    lastHumidity = humidity
                }
            }
            Sensor.TYPE_LIGHT -> {
                val light = event.values[0].toInt()
                if (light != lastLight) {
                    engine.onTrigger("SENSOR_LIGHT", light)
                    lastLight = light
                }
            }
            Sensor.TYPE_HEART_RATE -> {
                val heartRate = event.values[0].toInt()
                if (heartRate != lastHeartRate) {
                    engine.onTrigger("SENSOR_HEART_RATE", heartRate)
                    lastHeartRate = heartRate
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun updateOrientation() {
        val rotationMatrix = FloatArray(9)
        if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)) {
            val orientationAngles = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            val heading = Math.toDegrees(orientationAngles[0].toDouble()).toInt().let { if (it < 0) it + 360 else it }
            
            if (Math.abs(heading - lastHeading) >= 2) { // Only trigger if change is >= 2 degrees
                engine.onTrigger("COMPASS_HEADING", heading)
                lastHeading = heading
            }
            
            val strength = Math.sqrt((magnetometerReading[0] * magnetometerReading[0] + 
                                     magnetometerReading[1] * magnetometerReading[1] + 
                                     magnetometerReading[2] * magnetometerReading[2]).toDouble()).toInt()
            if (Math.abs(strength - lastMagneticStrength) >= 5) { // Only trigger if change is significant
                engine.onTrigger("SENSOR_MAGNETIC", strength)
                lastMagneticStrength = strength
            }
        }
    }

    private fun detectMovementPattern(values: FloatArray) {
        val x = values[0]; val y = values[1]; val z = values[2]
        val accel = Math.sqrt((x * x + y * y + z * z).toDouble()) - SensorManager.GRAVITY_EARTH
        
        val activity = when {
            accel > 8.0 -> "RUNNING"
            accel > 2.0 -> "WALKING"
            accel < 0.5 -> "STILL"
            else -> null
        }
        
        if (activity != null && activity != lastActivity) {
            engine.onTrigger("ACTIVITY_DETECTED", activity)
            lastActivity = activity
        }

        val pattern = when {
            accel > 15.0 -> "SHAKE"
            accel > 8.0 -> "JOLT"
            x > 5.0 -> "TILT_LEFT"
            x < -5.0 -> "TILT_RIGHT"
            else -> null
        }
        
        if (pattern != null && pattern != lastPattern) {
            engine.onTrigger("ACCELEROMETER_PATTERN", pattern)
            lastPattern = pattern
        }
    }

    private fun detectRotationPattern(values: FloatArray) {
        if (Math.abs(values[0]) + Math.abs(values[1]) + Math.abs(values[2]) > 5.0) {
            engine.onTrigger("GYROSCOPE_PATTERN", "ROTATION")
        }
    }

    private fun startPeriodicChecks() {
        periodicJob = scope.launch {
            var lastRx = android.net.TrafficStats.getTotalRxBytes()
            var lastTx = android.net.TrafficStats.getTotalTxBytes()
            var lastTime = System.currentTimeMillis()
            var lastOverlayPermission = Settings.canDrawOverlays(context)
            var lastUsedGB = -1L
            
            startNoiseMonitoring()
            
            while (isActive) {
                delay(5000)
                
                monitoredHosts.forEach { checkPing(it) }
                monitoredDomains.forEach { checkDomainReachability(it) }
                
                // Storage check
                val stats = android.os.StatFs(android.os.Environment.getExternalStorageDirectory().path)
                val usedGB = (stats.totalBytes - stats.availableBytes) / (1024 * 1024 * 1024)
                if (usedGB != lastUsedGB) {
                    engine.onTrigger("STORAGE_SIZE", usedGB.toInt())
                    lastUsedGB = usedGB
                }

                val currentOverlayPermission = Settings.canDrawOverlays(context)
                if (currentOverlayPermission != lastOverlayPermission) {
                    engine.onTrigger("OVERLAY_PERMISSION_CHANGED", currentOverlayPermission)
                    lastOverlayPermission = currentOverlayPermission
                }

                val curRx = android.net.TrafficStats.getTotalRxBytes()
                val curTx = android.net.TrafficStats.getTotalTxBytes()
                val curTime = System.currentTimeMillis()
                val speed = (curRx - lastRx) * 8 / ((curTime - lastTime) / 1000f) / 1_000_000f
                engine.onTrigger("NETWORK_SPEED_THRESHOLD", speed.toInt())
                lastRx = curRx; lastTx = curTx; lastTime = curTime
                
                checkNoiseLevel()
            }
        }
    }

    private fun startNoiseMonitoring() {
        if (context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return
        
        stopNoiseMonitoring()
        
        try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                android.media.MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                android.media.MediaRecorder()
            }.apply {
                setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                setOutputFormat(android.media.MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AMR_NB)
                
                // Using a cache file instead of /dev/null for better compatibility
                val outFile = File(context.cacheDir, "noise_monitor.3gp")
                setOutputFile(outFile.absolutePath)
                
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("TriggerManager", "Failed to start noise monitoring: ${e.message}")
            try {
                mediaRecorder?.release()
            } catch (_: Exception) {}
            mediaRecorder = null
        }
    }

    private fun checkNoiseLevel() {
        mediaRecorder?.let {
            val amplitude = it.maxAmplitude
            if (amplitude > 0) {
                val db = (20 * Math.log10(amplitude.toDouble())).toInt()
                engine.onTrigger("SENSOR_NOISE", db)
            }
        }
    }

    private fun stopNoiseMonitoring() {
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {}
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {}
        mediaRecorder = null
        
        try {
            File(context.cacheDir, "noise_monitor.3gp").delete()
        } catch (e: Exception) {}
    }

    private fun startScreenshotObserver() {
        screenshotObserver = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: android.net.Uri?) {
                if (uri?.toString()?.contains("screenshots", ignoreCase = true) == true) {
                    engine.onTrigger("SCREENSHOT_TAKEN")
                }
            }
        }
        context.contentResolver.registerContentObserver(
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            screenshotObserver!!
        )
    }

    private fun stopScreenshotObserver() {
        screenshotObserver?.let {
            context.contentResolver.unregisterContentObserver(it)
            screenshotObserver = null
        }
    }

    fun refreshMonitors() {
        scope.launch {
            val db = com.semseytech.rtsdevicesuitepro.automation.data.AutomationDatabase.getDatabase(context)
            val rules = db.automationDao().getAllRules()
            val enabledRules = rules.filter { it.isEnabled }
            Log.d("TriggerManager", "Refreshing monitors. Found ${enabledRules.size} enabled rules out of ${rules.size} total.")
            
            val pathsToWatch = mutableSetOf<String>()
            val hosts = mutableSetOf<String>()
            val domains = mutableSetOf<String>()
            val neededSensors = mutableSetOf<Int>()
            var needsLocation = false
            
            // Notify engine to refresh its cache
            engine.refreshRules()
            
            enabledRules.forEach { rule ->
                if (rule.trigger.requiredPermissions.any { it.contains("LOCATION") }) {
                    needsLocation = true
                }
                when (val t = rule.trigger) {
                    is com.semseytech.rtsdevicesuitepro.automation.models.Trigger.FileCreated -> pathsToWatch.add(t.path)
                    is com.semseytech.rtsdevicesuitepro.automation.models.Trigger.FileDeleted -> pathsToWatch.add(t.path)
                    is com.semseytech.rtsdevicesuitepro.automation.models.Trigger.FileModified -> pathsToWatch.add(t.path)
                    is com.semseytech.rtsdevicesuitepro.automation.models.Trigger.FolderChanged -> pathsToWatch.add(t.path)
                    is com.semseytech.rtsdevicesuitepro.automation.models.Trigger.PingStatus -> hosts.add(t.host)
                    is com.semseytech.rtsdevicesuitepro.automation.models.Trigger.DomainReachability -> domains.add(t.domain)
                    
                    is com.semseytech.rtsdevicesuitepro.automation.models.Trigger.StepCountThreshold -> neededSensors.add(Sensor.TYPE_STEP_COUNTER)
                    is com.semseytech.rtsdevicesuitepro.automation.models.Trigger.ProximityTriggered -> neededSensors.add(Sensor.TYPE_PROXIMITY)
                    is com.semseytech.rtsdevicesuitepro.automation.models.Trigger.ActivityDetected,
                    is com.semseytech.rtsdevicesuitepro.automation.models.Trigger.AccelerometerPattern -> neededSensors.add(Sensor.TYPE_ACCELEROMETER)
                    is com.semseytech.rtsdevicesuitepro.automation.models.Trigger.CompassHeading -> {
                        neededSensors.add(Sensor.TYPE_ACCELEROMETER)
                        neededSensors.add(Sensor.TYPE_MAGNETIC_FIELD)
                    }
                    is com.semseytech.rtsdevicesuitepro.automation.models.Trigger.MagnetometerThreshold -> neededSensors.add(Sensor.TYPE_MAGNETIC_FIELD)
                    is com.semseytech.rtsdevicesuitepro.automation.models.Trigger.GyroscopePattern -> neededSensors.add(Sensor.TYPE_GYROSCOPE)
                    is com.semseytech.rtsdevicesuitepro.automation.models.Trigger.AltitudeThreshold,
                    is com.semseytech.rtsdevicesuitepro.automation.models.Trigger.BarometerThreshold -> neededSensors.add(Sensor.TYPE_PRESSURE)
                    is com.semseytech.rtsdevicesuitepro.automation.models.Trigger.TemperatureThreshold -> neededSensors.add(Sensor.TYPE_AMBIENT_TEMPERATURE)
                    is com.semseytech.rtsdevicesuitepro.automation.models.Trigger.HumidityThreshold -> neededSensors.add(Sensor.TYPE_RELATIVE_HUMIDITY)
                    is com.semseytech.rtsdevicesuitepro.automation.models.Trigger.LightSensorThreshold -> neededSensors.add(Sensor.TYPE_LIGHT)
                    is com.semseytech.rtsdevicesuitepro.automation.models.Trigger.HeartRateThreshold -> neededSensors.add(Sensor.TYPE_HEART_RATE)
                    
                    else -> {}
                }
            }
            
            monitoredHosts.clear()
            monitoredHosts.addAll(hosts)
            monitoredDomains.clear()
            monitoredDomains.addAll(domains)
            
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                // Update Location Updates
                if (needsLocation) {
                    if (!isLocationUpdatesStarted) {
                        try {
                            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 10f, locationListener)
                            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 10f, locationListener)
                            isLocationUpdatesStarted = true
                            Log.d("TriggerManager", "Location updates started.")
                        } catch (e: SecurityException) {
                            Log.e("TriggerManager", "Failed to start location updates: permission missing")
                        }
                    }
                } else {
                    if (isLocationUpdatesStarted) {
                        locationManager.removeUpdates(locationListener)
                        isLocationUpdatesStarted = false
                        Log.d("TriggerManager", "Location updates stopped.")
                    }
                }

                // Update Sensors
                val sensorsToRemove = activeSensors.filter { !neededSensors.contains(it) }
                val sensorsToAdd = neededSensors.filter { !activeSensors.contains(it) }
                
                sensorsToRemove.forEach { type ->
                    sensorManager.getDefaultSensor(type)?.let { sensorManager.unregisterListener(this@TriggerManager, it) }
                    activeSensors.remove(type)
                }
                
                sensorsToAdd.forEach { type ->
                    sensorManager.getDefaultSensor(type)?.let {
                        sensorManager.registerListener(this@TriggerManager, it, SensorManager.SENSOR_DELAY_NORMAL)
                    }
                    activeSensors.add(type)
                }

                // Stop observers for paths no longer needed
                val currentObservedPaths = fileObservers.keys.toList()
                currentObservedPaths.forEach { path ->
                    if (!pathsToWatch.contains(path)) {
                        fileObservers[path]?.stopWatching()
                        fileObservers.remove(path)
                    }
                }
                
                // Start observers for new paths
                pathsToWatch.forEach { path ->
                    if (!fileObservers.containsKey(path) && path.isNotEmpty()) {
                        try {
                            val observer = object : FileObserver(path, CREATE or DELETE or MODIFY or MOVED_TO or MOVED_FROM) {
                                override fun onEvent(event: Int, fileName: String?) {
                                    if (fileName == null) return
                                    val fullPath = if (path.endsWith("/")) "$path$fileName" else "$path/$fileName"
                                    when {
                                        (event and CREATE) != 0 || (event and MOVED_TO) != 0 -> engine.onTrigger("FILE_CREATED", fullPath)
                                        (event and DELETE) != 0 || (event and MOVED_FROM) != 0 -> engine.onTrigger("FILE_DELETED", fullPath)
                                        (event and MODIFY) != 0 -> engine.onTrigger("FILE_MODIFIED", fullPath)
                                    }
                                    engine.onTrigger("FOLDER_CHANGED", path)
                                }
                            }
                            observer.startWatching()
                            fileObservers[path] = observer
                        } catch (e: Exception) {
                            Log.e("TriggerManager", "Failed to watch path: $path", e)
                        }
                    }
                }
            }
        }
    }

    private fun checkPing(host: String) {
        var process: Process? = null
        try {
            process = Runtime.getRuntime().exec("ping -c 1 -w 2 $host")
            val status = if (process.waitFor() == 0) "SUCCESS" else "FAILURE"
            engine.onTrigger("PING_STATUS", Pair(host, status))
        } catch (e: Exception) { 
            engine.onTrigger("PING_STATUS", Pair(host, "FAILURE")) 
        } finally {
            try {
                process?.inputStream?.close()
                process?.outputStream?.close()
                process?.errorStream?.close()
            } catch (e: Exception) {}
            process?.destroy()
        }
    }

    private fun checkDomainReachability(domain: String) {
        scope.launch {
            try {
                val status = if (InetAddress.getByName(domain).isReachable(3000)) "REACHABLE" else "UNREACHABLE"
                engine.onTrigger("DOMAIN_REACHABILITY", Pair(domain, status))
            } catch (e: Exception) { engine.onTrigger("DOMAIN_REACHABILITY", Pair(domain, "UNREACHABLE")) }
        }
    }

    private fun checkIpAddressChange() {
        val currentIp = getLocalIpAddress()
        if (currentIp != null && currentIp != lastIpAddress) {
            lastIpAddress = currentIp
            engine.onTrigger("IP_ADDRESS_CHANGED", currentIp)
        }
    }

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            while (interfaces.hasMoreElements()) {
                val addrs = interfaces.nextElement().inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && !addr.isLinkLocalAddress) return addr.hostAddress
                }
            }
        } catch (e: Exception) { Log.e("TriggerManager", "Error getting IP address", e) }
        return null
    }

    @SuppressLint("MissingPermission")
    fun start() {
        Log.d("TriggerManager", "Registering listeners...")
        
        // Refresh monitors based on current rules
        refreshMonitors()
        startScreenshotObserver()

        val filter = IntentFilter().apply {
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(WifiManager.RSSI_CHANGED_ACTION)
            addAction("android.net.wifi.WIFI_AP_STATE_CHANGED")
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_BOOT_COMPLETED)
            addAction(Intent.ACTION_SHUTDOWN)
            addAction(Intent.ACTION_REBOOT)
            addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(Intent.ACTION_CONFIGURATION_CHANGED)
            addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction("android.media.VOLUME_CHANGED_ACTION")
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            addAction(android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        
        val mediaFilter = IntentFilter().apply {
            addAction(Intent.ACTION_MEDIA_MOUNTED)
            addAction(Intent.ACTION_MEDIA_UNMOUNTED)
            addAction(Intent.ACTION_MEDIA_EJECT)
            addDataScheme("file")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, mediaFilter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, mediaFilter)
        }
        
        val packageFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        context.registerReceiver(receiver, packageFilter)

        val commFilter = IntentFilter().apply {
            addAction("android.provider.Telephony.SMS_RECEIVED")
            addAction("android.provider.Telephony.WAP_PUSH_RECEIVED")
            addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                addAction(TelephonyManager.ACTION_SHOW_VOICEMAIL_NOTIFICATION)
            }
            priority = 999
        }
        context.registerReceiver(receiver, commFilter)

        connectivityManager.registerNetworkCallback(NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(), networkCallback)
        
        startPeriodicChecks()
        clipboardManager.addPrimaryClipChangedListener(clipboardListener)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.registerAudioPlaybackCallback(playbackCallback, null)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            audioManager.registerAudioRecordingCallback(recordingCallback, null)
        }
        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(sessionListener, null)
        } catch (e: Exception) {
            Log.w("TriggerManager", "Could not register session listener: ${e.message}")
        }

        Log.d("TriggerManager", "Listeners registered.")
    }

    fun stop() {
        Log.d("TriggerManager", "Unregistering listeners...")
        try {
            periodicJob?.cancel()
            stopNoiseMonitoring()
            stopScreenshotObserver()
            fileObservers.values.forEach { it.stopWatching() }
            fileObservers.clear()
            context.unregisterReceiver(receiver)
            connectivityManager.unregisterNetworkCallback(networkCallback)
            if (isLocationUpdatesStarted) {
                locationManager.removeUpdates(locationListener)
                isLocationUpdatesStarted = false
            }
            sensorManager.unregisterListener(this)
            clipboardManager.removePrimaryClipChangedListener(clipboardListener)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioManager.unregisterAudioPlaybackCallback(playbackCallback)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                audioManager.unregisterAudioRecordingCallback(recordingCallback)
            }
            mediaSessionManager.removeOnActiveSessionsChangedListener(sessionListener)
        } catch (e: Exception) { Log.w("TriggerManager", "Error unregistering: ${e.message}") }
    }
}

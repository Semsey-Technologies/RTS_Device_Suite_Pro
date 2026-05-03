package com.semseytech.rtsdevicesuitepro.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class CaptivePortalChecker {
    private val CHECK_URL = "http://connectivitycheck.gstatic.com/generate_204"

    suspend fun isCaptive(): Boolean = withContext(Dispatchers.IO) {
        var urlConnection: HttpURLConnection? = null
        try {
            val url = URL(CHECK_URL)
            urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.instanceFollowRedirects = false
            urlConnection.connectTimeout = 5000
            urlConnection.readTimeout = 5000
            urlConnection.useCaches = false
            urlConnection.connect()
            
            // 204 means no content and successful connection
            // Anything else (like 302 redirect or 200 with content) indicates a portal
            urlConnection.responseCode != 204
        } catch (e: Exception) {
            false
        } finally {
            urlConnection?.disconnect()
        }
    }
}

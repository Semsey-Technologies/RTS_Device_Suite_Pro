package com.semseytech.rtsdevicesuitepro.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.InetAddress
import kotlin.system.measureTimeMillis

class DnsBenchmark {

    private val testDnsServers = mapOf(
        "Cloudflare" to "1.1.1.1",
        "Google" to "8.8.8.8",
        "Quad9" to "9.9.9.9",
        "OpenDNS" to "208.67.222.222"
    )

    suspend fun benchmark(): List<DnsResult> = withContext(Dispatchers.IO) {
        testDnsServers.map { (name, ip) ->
            async {
                testDns(name, ip)
            }
        }.awaitAll()
    }

    private fun testDns(name: String, ip: String): DnsResult {
        return try {
            var totalLatency = 0L
            var successCount = 0
            val samples = 5
            
            repeat(samples) {
                val start = System.currentTimeMillis()
                // Use a different host each time to avoid local cache if possible
                val testHost = when(it) {
                    0 -> "google.com"
                    1 -> "cloudflare.com"
                    2 -> "github.com"
                    3 -> "microsoft.com"
                    else -> "amazon.com"
                }
                try {
                    val resolved = InetAddress.getByName(testHost)
                    if (resolved != null) {
                        totalLatency += (System.currentTimeMillis() - start)
                        successCount++
                    }
                } catch (e: Exception) {}
                Thread.sleep(50)
            }

            DnsResult(
                name = name,
                ip = ip,
                latency = if (successCount > 0) totalLatency / successCount else 999L,
                isSuccess = successCount >= 1 // At least one success
            )
        } catch (e: Exception) {
            DnsResult(name, ip, 999L, false)
        }
    }
}

package com.semseytech.rtsdevicesuitepro.net

data class NetworkInfo(
    val localIp: String = "Unknown",
    val externalIp: String = "Fetching...",
    val gateway: String = "Unknown",
    val subnetMask: String = "Unknown",
    val dnsServers: List<String> = emptyList(),
    val linkSpeed: Int = 0, // Mbps
    val frequency: Int = 0, // MHz
    val channel: Int = 0,
    val rssi: Int = 0, // dBm
    val snr: Int? = null,
    val networkType: String = "Unknown",
    val ssid: String = "Unknown",
    val bssid: String = "Unknown",
    val isVpn: Boolean = false,
    val isMetered: Boolean = false,
    val isRoaming: Boolean = false,
    val isValidated: Boolean = false,
    val isCaptive: Boolean = false,
    val location: String = "Unknown",
    val isp: String = "Unknown",
    // Cellular specific
    val carrierName: String = "Unknown",
    val cellNetworkType: String = "Unknown",
    val cellDbm: Int? = null,
    val mobileDataEnabled: Boolean = false
)

data class WifiScanResult(
    val ssid: String,
    val level: Int,
    val frequency: Int,
    val channel: Int
)

data class QualityMetrics(
    val latencyAvg: Long = 0,
    val latencyMin: Long = 0,
    val latencyMax: Long = 0,
    val jitter: Long = 0,
    val packetLoss: Float = 0f,
    val dnsResolutionTime: Long = 0,
    val latencyHistory: List<Long> = emptyList()
)

data class DnsResult(
    val name: String,
    val ip: String,
    val latency: Long,
    val isSuccess: Boolean
)

data class OptimizationResult(
    val baselineLatency: Long,
    val newLatency: Long,
    val dnsChanged: Boolean,
    val fastestDns: String?,
    val socketsFlushed: Boolean,
    val networkRebound: Boolean
)

data class NetOptimizerState(
    val networkInfo: NetworkInfo = NetworkInfo(),
    val qualityMetrics: QualityMetrics = QualityMetrics(),
    val dnsResults: List<DnsResult> = emptyList(),
    val isOptimizing: Boolean = false,
    val optimizationLog: List<String> = emptyList(),
    val lastOptimization: OptimizationResult? = null
)

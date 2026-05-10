package com.semseytech.rtsdevicesuitepro.model

import android.os.Build

object ModelIntelligence {
    data class DeviceSpec(
        val manufacturer: String,
        val model: String,
        val thermalThreshold: Float = 60f,
        val batteryIdealTempMin: Float = 15f,
        val batteryIdealTempMax: Float = 35f,
        val knownWeakPoints: String = "None known.",
        val typicalBatteryAging: String = "Standard Lithium-ion aging profile.",
        val chargingBestPractices: String = "Maintain 20-80% charge where possible.",
        val notes: String = "",
        val restoreNotes: String = "",
        val quirks: String = "",
        val storageGuidance: String = "Standard storage maintenance.",
        val networkGuidance: String = "Standard network maintenance.",
        val wipeGuidance: String = "Standard secure wipe procedures recommended.",
        val hardwareWeakPoints: String = "None known.",
        val replacementIntervalMonths: Int = 36
    )

    private val specs = listOf(
        DeviceSpec(
            "Google", "Pixel 7", 
            thermalThreshold = 55f, 
            quirks = "Known Wi-Fi 6E performance quirks. Tensor G2 can run warm during 4K recording.",
            networkGuidance = "If Wi-Fi 6E is unstable, try resetting network settings or using 5GHz band.",
            hardwareWeakPoints = "Fragile camera bar glass - use a protective case.",
            knownWeakPoints = "Optical fingerprint sensor speed, modem efficiency in low signal.",
            typicalBatteryAging = "80% capacity typically reached after 800 cycles.",
            chargingBestPractices = "Adaptive Charging is highly recommended for overnight use.",
            wipeGuidance = "Fast NVMe storage: single-pass random overwrite followed by zeroing is highly effective.",
            replacementIntervalMonths = 48
        ),
        DeviceSpec(
            "Samsung", "SM-G9", 
            thermalThreshold = 65f, 
            notes = "Aggressive file caching detected. Regular deep clean recommended.",
            storageGuidance = "Samsung Gallery cache can grow large. Use Deep Clean monthly.",
            restoreNotes = "May require re-enabling Knox features after major restores.",
            knownWeakPoints = "Battery swelling risk if stored at 100% for months. Screen burn-in on older AMOLED.",
            typicalBatteryAging = "Fast charging increases heat; 85% limit (Protect Battery) recommended.",
            chargingBestPractices = "Use 'Protect Battery' feature to limit charge to 85% for longevity.",
            wipeGuidance = "Samsung KNOX may flag unusual storage patterns; use standard API methods.",
            replacementIntervalMonths = 36
        ),
        DeviceSpec(
            "Xiaomi", "M21", 
            notes = "Weekly backup recommended - call logs may be wiped during updates.",
            restoreNotes = "Check 'Install via USB' in Developer Options if batch install fails.",
            quirks = "Aggressive background process killing (MIUI/HyperOS).",
            hardwareWeakPoints = "Proximity sensor issues reported on some batches.",
            typicalBatteryAging = "High-wattage charging (120W+) may accelerate aging if used daily.",
            replacementIntervalMonths = 30
        ),
        DeviceSpec(
            "Motorola", "moto", 
            restoreNotes = "Requires reboot after APK batch install",
            hardwareWeakPoints = "Charging port can be sensitive to dust - clean monthly.",
            knownWeakPoints = "Slow security patch updates on budget models.",
            typicalBatteryAging = "Standard 500-cycle life expectancy for budget series.",
            replacementIntervalMonths = 24
        ),
        DeviceSpec(
            "Google", "Pixel 6",
            thermalThreshold = 52f,
            quirks = "First-gen Tensor thermal throttling. Modem power draw is high.",
            hardwareWeakPoints = "Fingerprint sensor reliability issues.",
            typicalBatteryAging = "Expect significant capacity drop after 2 years of heavy use.",
            replacementIntervalMonths = 36
        )
    )

    fun getSpec(manufacturer: String, model: String): DeviceSpec {
        return specs.find { 
            manufacturer.contains(it.manufacturer, ignoreCase = true) && 
            model.contains(it.model, ignoreCase = true) 
        } ?: DeviceSpec(manufacturer, model) // Default
    }
}

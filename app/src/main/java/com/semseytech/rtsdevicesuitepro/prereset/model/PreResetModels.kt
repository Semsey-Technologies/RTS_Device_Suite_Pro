package com.semseytech.rtsdevicesuitepro.prereset.model

import androidx.compose.ui.graphics.Color

data class AccountInfo(
    val type: String,
    val name: String,
    val packageName: String?,
    val isTwoFactorEnabled: Boolean = false,
    val isWebBased: Boolean = false
)

data class AppSafetyInfo(
    val name: String,
    val packageName: String,
    val requiresLogin: Boolean,
    val storesLocalData: Boolean,
    val isTwoFactorApp: Boolean,
    val requiresManualExport: Boolean,
    val hasBackupApi: Boolean,
    val category: AppCategory
)

enum class AppCategory {
    BANKING, SOCIAL, MESSAGING, CLOUD, EMAIL, PASSWORD_MANAGER, TWO_FACTOR, CRYPTO, SHOPPING, OTHER
}

data class PreResetGuideData(
    val accounts: List<AccountInfo>,
    val apps: List<AppSafetyInfo>,
    val timestamp: Long = System.currentTimeMillis(),
    val deviceModel: String = android.os.Build.MODEL,
    val androidVersion: String = android.os.Build.VERSION.RELEASE
)

enum class SafetyStatus(val color: Color) {
    OK(Color(0xFF00FF99)),
    WARNING(Color(0xFFFFCC00)),
    CRITICAL(Color(0xFFFF3333))
}

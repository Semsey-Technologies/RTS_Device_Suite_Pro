package com.semseytech.rtsdevicesuitepro.prereset.logic

import android.accounts.AccountManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.semseytech.rtsdevicesuitepro.prereset.model.*

class PreResetScanner(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    fun scanDevice(): PreResetGuideData {
        return PreResetGuideData(
            accounts = scanAccounts(),
            apps = scanApps()
        )
    }

    private fun scanAccounts(): List<AccountInfo> {
        val am = AccountManager.get(context)
        return am.accounts.map { account ->
            AccountInfo(
                type = account.type,
                name = account.name,
                packageName = getPackageForAccountType(account.type)
            )
        }
    }

    private fun scanApps(): List<AppSafetyInfo> {
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        return installedApps
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 } // Non-system apps
            .map { appInfo ->
                val packageName = appInfo.packageName
                val name = packageManager.getApplicationLabel(appInfo).toString()
                val category = detectCategory(packageName)
                
                AppSafetyInfo(
                    name = name,
                    packageName = packageName,
                    requiresLogin = requiresLogin(packageName, category),
                    storesLocalData = storesLocalData(packageName, category),
                    isTwoFactorApp = category == AppCategory.TWO_FACTOR,
                    requiresManualExport = requiresManualExport(packageName, category),
                    hasBackupApi = appInfo.backupAgentName != null,
                    category = category
                )
            }
    }

    private fun getPackageForAccountType(type: String): String? {
        return when {
            type.contains("google", ignoreCase = true) -> "com.google.android.gms"
            type.contains("samsung", ignoreCase = true) -> "com.samsung.android.messaging"
            type.contains("microsoft", ignoreCase = true) -> "com.microsoft.office.outlook"
            type.contains("facebook", ignoreCase = true) -> "com.facebook.katana"
            type.contains("whatsapp", ignoreCase = true) -> "com.whatsapp"
            else -> null
        }
    }

    private fun detectCategory(packageName: String): AppCategory {
        return when {
            packageName.contains("bank") || packageName.contains("finance") || packageName.contains("wallet") -> AppCategory.BANKING
            packageName.contains("auth") || packageName.contains("otp") || packageName.contains("2fa") || 
                known2FAPackages.contains(packageName) -> AppCategory.TWO_FACTOR
            packageName.contains("crypto") || packageName.contains("coin") -> AppCategory.CRYPTO
            packageName.contains("pass") || packageName.contains("safe") -> AppCategory.PASSWORD_MANAGER
            packageName.contains("mail") || packageName.contains("email") -> AppCategory.EMAIL
            packageName.contains("cloud") || packageName.contains("drive") || packageName.contains("dropbox") -> AppCategory.CLOUD
            packageName.contains("chat") || packageName.contains("messenger") || packageName.contains("whatsapp") || 
                packageName.contains("telegram") || packageName.contains("signal") -> AppCategory.MESSAGING
            packageName.contains("social") || packageName.contains("facebook") || packageName.contains("twitter") || 
                packageName.contains("instagram") || packageName.contains("tiktok") -> AppCategory.SOCIAL
            packageName.contains("shop") || packageName.contains("amazon") || packageName.contains("ebay") -> AppCategory.SHOPPING
            else -> AppCategory.OTHER
        }
    }

    private fun requiresLogin(packageName: String, category: AppCategory): Boolean {
        return category != AppCategory.OTHER || packageName.contains("login") || packageName.contains("account")
    }

    private fun storesLocalData(packageName: String, category: AppCategory): Boolean {
        // High level heuristic: most messaging and security apps store local data
        return category == AppCategory.MESSAGING || category == AppCategory.TWO_FACTOR || 
               category == AppCategory.CRYPTO || category == AppCategory.PASSWORD_MANAGER
    }

    private fun requiresManualExport(packageName: String, category: AppCategory): Boolean {
        // 2FA and Crypto usually require manual export
        return category == AppCategory.TWO_FACTOR || category == AppCategory.CRYPTO
    }

    companion object {
        private val known2FAPackages = setOf(
            "com.google.android.apps.authenticator2",
            "com.microsoft.android.authenticator",
            "com.authy.authy",
            "com.duosecurity.duomobile",
            "org.fedorahosted.freeotp",
            "com.valvesoftware.android.steam.community"
        )
    }
}

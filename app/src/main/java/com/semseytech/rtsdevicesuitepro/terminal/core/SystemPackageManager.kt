package com.semseytech.rtsdevicesuitepro.terminal.core

import com.google.gson.Gson
import com.semseytech.rtsdevicesuitepro.organizer.worker.ExtractionUtils
import com.semseytech.rtsdevicesuitepro.terminal.TerminalEnv
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SystemPackage(
    val name: String,
    val version: String,
    val description: String,
    val binFiles: List<String> = emptyList(),
    val libFiles: List<String> = emptyList(),
    val dependencies: List<String> = emptyList()
)

class SystemPackageManager(private val env: ShellEnvironment) {
    private val gson = Gson()
    private val installedFile = File(TerminalEnv.packagesDir, "installed.json")

    init {
        updatePath()
    }

    private fun getInstalledPackages(): MutableMap<String, SystemPackage> {
        if (!installedFile.exists()) return mutableMapOf()
        return try {
            val type = object : com.google.gson.reflect.TypeToken<MutableMap<String, SystemPackage>>() {}.type
            gson.fromJson(installedFile.readText(), type)
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    private fun saveInstalledPackages(packages: Map<String, SystemPackage>) {
        installedFile.writeText(gson.toJson(packages))
    }

    suspend fun install(packageName: String): String = withContext(Dispatchers.IO) {
        val installed = getInstalledPackages()
        if (installed.containsKey(packageName)) {
            return@withContext "$packageName is already installed."
        }

        val pkgDir = File(TerminalEnv.packagesDir, packageName)
        val assetPath = "packages/$packageName.zip"
        val tempZip = File(TerminalEnv.tmpDir, "$packageName.zip")

        try {
            // Local extraction from assets
            val assetManager = TerminalEnv.getAssetManager()
            try {
                assetManager.open(assetPath).use { input ->
                    FileOutputStream(tempZip).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                return@withContext "ERROR: Package '$packageName' not found in local bundles."
            }

            ExtractionUtils.extract(tempZip, pkgDir)
            tempZip.delete()

            val manifestFile = File(pkgDir, "manifest.json")
            if (!manifestFile.exists()) {
                pkgDir.deleteRecursively()
                return@withContext "ERROR: Invalid package bundle: manifest.json missing"
            }
            val pkg = gson.fromJson(manifestFile.readText(), SystemPackage::class.java)

            // Register binaries
            val binDir = File(pkgDir, "bin")
            if (binDir.exists() && binDir.isDirectory) {
                binDir.listFiles()?.forEach { bin ->
                    val wrapper = File(TerminalEnv.binDir, bin.name)
                    val libPath = File(pkgDir, "lib").absolutePath
                    wrapper.writeText("#!/system/bin/sh\nexport LD_LIBRARY_PATH=$libPath:\$LD_LIBRARY_PATH\nexec ${bin.absolutePath} \"\$@\"\n")
                    wrapper.setExecutable(true)
                    bin.setExecutable(true)
                }
            }
            
            // Link libraries
            val pkgLibDir = File(pkgDir, "lib")
            if (pkgLibDir.exists() && pkgLibDir.isDirectory) {
                pkgLibDir.listFiles()?.forEach { lib ->
                    val target = File(TerminalEnv.libDir, lib.name)
                    lib.copyTo(target, overwrite = true)
                }
            }

            installed[packageName] = pkg
            saveInstalledPackages(installed)
            updatePath()

            "Successfully installed $packageName ${pkg.version}"
        } catch (e: Exception) {
            "ERROR: Installation failed: ${e.message}"
        }
    }

    suspend fun uninstall(packageName: String): String = withContext(Dispatchers.IO) {
        val installed = getInstalledPackages()
        val pkg = installed[packageName] ?: return@withContext "$packageName is not installed."

        val pkgDir = File(TerminalEnv.packagesDir, packageName)
        
        File(pkgDir, "bin").listFiles()?.forEach { File(TerminalEnv.binDir, it.name).delete() }
        File(pkgDir, "lib").listFiles()?.forEach { File(TerminalEnv.libDir, it.name).delete() }

        pkgDir.deleteRecursively()
        installed.remove(packageName)
        saveInstalledPackages(installed)
        
        "Successfully uninstalled $packageName"
    }

    fun list(): String {
        val installed = getInstalledPackages()
        val bundled = try { TerminalEnv.getAssetManager().list("packages")?.map { it.removeSuffix(".zip") } ?: emptyList() } catch(e: Exception) { emptyList() }
        
        val sb = StringBuilder()
        if (installed.isNotEmpty()) {
            sb.append("Installed:\n")
            installed.values.forEach { sb.append("- ${it.name} v${it.version}\n") }
        }
        
        val available = bundled.filter { !installed.containsKey(it) }
        if (available.isNotEmpty()) {
            sb.append("\nAvailable (Local):\n")
            available.forEach { sb.append("- $it\n") }
        }
        
        return if (sb.isEmpty()) "No packages available." else sb.toString().trim()
    }

    fun updatePath() {
        val installed = getInstalledPackages()
        val paths = mutableListOf(TerminalEnv.binDir.absolutePath, File(TerminalEnv.pythonDir, "bin").absolutePath)
        installed.keys.forEach { paths.add(File(TerminalEnv.packagesDir, "$it/bin").absolutePath) }
        paths.addAll(listOf("/system/bin", "/system/xbin"))
        env.setVariable("PATH", paths.joinToString(":"))
        
        val libs = mutableListOf(TerminalEnv.libDir.absolutePath)
        installed.keys.forEach { 
            val p = File(TerminalEnv.packagesDir, "$it/lib").absolutePath
            if (File(p).exists()) libs.add(p)
        }
        env.setVariable("LD_LIBRARY_PATH", libs.joinToString(":"))
    }
}

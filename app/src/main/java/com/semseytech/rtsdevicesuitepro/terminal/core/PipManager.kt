package com.semseytech.rtsdevicesuitepro.terminal.core

import com.semseytech.rtsdevicesuitepro.organizer.worker.ExtractionUtils
import com.semseytech.rtsdevicesuitepro.terminal.TerminalEnv
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PipManager {
    
    suspend fun execute(args: List<String>): String = withContext(Dispatchers.IO) {
        if (args.isEmpty()) return@withContext "Usage: pip <command> [options]"
        
        val command = args[0]
        return@withContext when (command) {
            "install" -> install(args.drop(1))
            "uninstall" -> uninstall(args.drop(1))
            "list" -> list()
            "show" -> show(args.drop(1))
            else -> "ERROR: Unknown command \"$command\""
        }
    }

    private suspend fun install(packages: List<String>): String = withContext(Dispatchers.IO) {
        if (packages.isEmpty()) return@withContext "ERROR: You must give at least one requirement to install"
        
        val result = StringBuilder()
        val gson = Gson()
        val queue = ArrayDeque<String>()
        queue.addAll(packages)
        val processed = mutableSetOf<String>()

        while (queue.isNotEmpty()) {
            val pkgRequirement = queue.removeFirst()
            // Extract package name from requirement (e.g., requests>=2.0.0 -> requests)
            val cleanName = pkgRequirement.split(Regex("[=><!]"))[0].trim().lowercase()
            
            if (processed.contains(cleanName)) continue
            processed.add(cleanName)

            // Check if already satisfied by checking for .dist-info directory
            val normalizedSearchName = cleanName.replace("-", "_")
            val distInfoDir = TerminalEnv.sitePackagesDir.listFiles()?.find { 
                it.isDirectory && it.name.lowercase().startsWith(normalizedSearchName) && it.name.contains(".dist-info") 
            }
            if (distInfoDir != null) {
                result.append("Requirement already satisfied: $cleanName in ${TerminalEnv.sitePackagesDir.absolutePath}\n")
                continue
            }

            try {
                result.append("Collecting $cleanName\n")
                val pypiUrl = "https://pypi.org/pypi/$cleanName/json"
                val response = try {
                    URL(pypiUrl).readText()
                } catch (e: Exception) {
                    result.append("ERROR: Could not find a version that satisfies the requirement $cleanName\n")
                    continue
                }
                
                val json = gson.fromJson(response, JsonObject::class.java)
                val info = json.getAsJsonObject("info")
                val version = info.get("version").asString
                
                // Add dependencies to queue
                val requiresDist = info.get("requires_dist")
                if (requiresDist != null && !requiresDist.isJsonNull) {
                    requiresDist.asJsonArray.forEach { dep ->
                        val depStr = dep.asString
                        // Basic filtering for environment markers and extras
                        if (!depStr.contains("extra ==") && !depStr.contains(";")) {
                            queue.add(depStr)
                        }
                    }
                }

                // Find a wheel (bdist_wheel) or source distribution (sdist)
                val urls = json.getAsJsonArray("urls")
                var downloadUrl: String? = null
                var filename: String? = null
                
                for (urlElement in urls) {
                    val urlObj = urlElement.asJsonObject
                    if (urlObj.get("packagetype").asString == "bdist_wheel") {
                        downloadUrl = urlObj.get("url").asString
                        filename = urlObj.get("filename").asString
                        break
                    }
                }
                
                if (downloadUrl == null) {
                    for (urlElement in urls) {
                        val urlObj = urlElement.asJsonObject
                        if (urlObj.get("packagetype").asString == "sdist") {
                            downloadUrl = urlObj.get("url").asString
                            filename = urlObj.get("filename").asString
                            break
                        }
                    }
                }
                
                if (downloadUrl != null && filename != null) {
                    result.append("  Downloading $filename...\n")
                    val tempFile = File(TerminalEnv.tmpDir, filename)
                    URL(downloadUrl).openStream().use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    result.append("  Installing collected packages: $cleanName\n")
                    ExtractionUtils.extract(tempFile, TerminalEnv.sitePackagesDir)
                    tempFile.delete()
                    
                    result.append("Successfully installed $cleanName-$version\n")
                } else {
                    result.append("ERROR: No suitable distribution found for $cleanName\n")
                }
            } catch (e: Exception) {
                result.append("ERROR: Exception during installation of $cleanName: ${e.message}\n")
            }
        }
        return@withContext result.toString().trim()
    }

    private fun uninstall(packages: List<String>): String {
        if (packages.isEmpty()) return "ERROR: You must give at least one requirement to uninstall"
        
        val result = StringBuilder()
        packages.forEach { pkgName ->
            val cleanName = pkgName.lowercase().replace("-", "_")
            // Try to find directories related to the package
            val dirsToDelete = TerminalEnv.sitePackagesDir.listFiles()?.filter { 
                it.isDirectory && (it.name.lowercase() == cleanName || it.name.lowercase().startsWith("$cleanName-") || it.name.lowercase().startsWith("${cleanName}_"))
            } ?: emptyList()
            
            if (dirsToDelete.isNotEmpty()) {
                dirsToDelete.forEach { it.deleteRecursively() }
                result.append("Successfully uninstalled $pkgName\n")
            } else {
                result.append("Skipping $pkgName as it is not installed.\n")
            }
        }
        return result.toString().trim()
    }

    private fun list(): String {
        val pkgs = TerminalEnv.sitePackagesDir.listFiles()?.filter { it.isDirectory && it.name.endsWith(".dist-info") }
        if (pkgs.isNullOrEmpty()) return "No packages installed."
        
        val output = StringBuilder("Package".padEnd(20)).append("Version\n").append("-".repeat(30)).append("\n")
        pkgs.sortedBy { it.name.lowercase() }.forEach { dir ->
            val parts = dir.name.substringBefore(".dist-info").split("-")
            val name = parts[0]
            val version = if (parts.size > 1) parts[1] else "unknown"
            output.append(name.padEnd(20)).append(version).append("\n")
        }
        return output.toString().trim()
    }

    private fun show(packages: List<String>): String {
        if (packages.isEmpty()) return "ERROR: Please provide a package name"
        val pkgName = packages[0].lowercase().replace("-", "_")
        val distInfoDir = TerminalEnv.sitePackagesDir.listFiles()?.find { 
            it.isDirectory && it.name.lowercase().startsWith(pkgName) && it.name.endsWith(".dist-info") 
        }
        
        return if (distInfoDir != null) {
            val metadataFile = File(distInfoDir, "METADATA")
            val metadata = if (metadataFile.exists()) {
                metadataFile.readLines().filter { line ->
                    listOf("Name:", "Version:", "Summary:", "Home-page:", "Author:").any { line.startsWith(it) }
                }.joinToString("\n")
            } else "No METADATA file found in .dist-info"
            "Location: ${TerminalEnv.sitePackagesDir.absolutePath}\n$metadata"
        } else {
            "Package(s) not found: $pkgName"
        }
    }
}

package com.semseytech.rtsdevicesuitepro.terminal.core

import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class BusyBoxLayer(private val env: ShellEnvironment) {

    fun execute(command: String, args: List<String>): String {
        return when (command) {
            "ls" -> ls(args)
            "cat" -> cat(args)
            "pwd" -> env.currentWorkingDirectory.absolutePath
            "mkdir" -> mkdir(args)
            "rm" -> rm(args)
            "cp" -> cp(args)
            "mv" -> mv(args)
            "echo" -> args.joinToString(" ")
            "date" -> SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US).format(Date())
            "touch" -> touch(args)
            "chmod" -> "chmod: Operation not permitted without root"
            "whoami" -> env.getVariable("USER")
            "uname" -> "Linux rts-android 5.10.0-generic"
            else -> throw IllegalArgumentException("Unknown BusyBox command: $command")
        }
    }

    private fun ls(args: List<String>): String {
        val path = if (args.isNotEmpty() && !args[0].startsWith("-")) args[0] else "."
        val dir = env.resolvePath(path)
        if (!dir.exists()) return "ls: $path: No such file or directory"
        if (!dir.isDirectory) return dir.name

        val files = dir.listFiles()?.sortedBy { it.name } ?: return ""
        val showAll = args.contains("-a")
        val longFormat = args.contains("-l")

        val result = StringBuilder()
        files.filter { showAll || !it.name.startsWith(".") }.forEach { file ->
            if (longFormat) {
                val type = if (file.isDirectory) "d" else "-"
                val size = file.length().toString().padStart(10)
                val date = SimpleDateFormat("MMM dd HH:mm", Locale.US).format(Date(file.lastModified()))
                result.append("$type rwxr-xr-x $size $date ${file.name}\n")
            } else {
                result.append("${file.name}  ")
            }
        }
        return result.toString().trim()
    }

    private fun cat(args: List<String>): String {
        if (args.isEmpty()) return ""
        val file = env.resolvePath(args[0])
        return if (file.exists() && file.isFile) {
            file.readText()
        } else {
            "cat: ${args[0]}: No such file or directory"
        }
    }

    private fun mkdir(args: List<String>): String {
        if (args.isEmpty()) return "mkdir: missing operand"
        val dir = env.resolvePath(args[0])
        return if (dir.mkdirs()) "" else "mkdir: cannot create directory '${args[0]}'"
    }

    private fun rm(args: List<String>): String {
        if (args.isEmpty()) return "rm: missing operand"
        val recursive = args.contains("-r") || args.contains("-rf")
        val file = env.resolvePath(args.last())
        
        return if (file.exists()) {
            if (recursive) {
                file.deleteRecursively()
                ""
            } else {
                if (file.isDirectory) "rm: cannot remove '${file.name}': Is a directory"
                else { file.delete(); "" }
            }
        } else {
            "rm: cannot remove '${args.last()}': No such file or directory"
        }
    }

    private fun touch(args: List<String>): String {
        if (args.isEmpty()) return "touch: missing file operand"
        val file = env.resolvePath(args[0])
        if (!file.exists()) file.createNewFile()
        else file.setLastModified(System.currentTimeMillis())
        return ""
    }

    private fun cp(args: List<String>): String {
        if (args.size < 2) return "cp: missing destination file operand after '${args.getOrNull(0)}'"
        val src = env.resolvePath(args[0])
        val dest = env.resolvePath(args[1])
        if (!src.exists()) return "cp: cannot stat '${args[0]}': No such file or directory"
        
        val finalDest = if (dest.isDirectory) File(dest, src.name) else dest
        src.copyTo(finalDest, overwrite = true)
        return ""
    }

    private fun mv(args: List<String>): String {
        if (args.size < 2) return "mv: missing destination file operand after '${args.getOrNull(0)}'"
        val src = env.resolvePath(args[0])
        val dest = env.resolvePath(args[1])
        if (!src.exists()) return "mv: cannot stat '${args[0]}': No such file or directory"

        val finalDest = if (dest.isDirectory) File(dest, src.name) else dest
        src.renameTo(finalDest)
        return ""
    }
}

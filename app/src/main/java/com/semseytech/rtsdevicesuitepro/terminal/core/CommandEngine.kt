package com.semseytech.rtsdevicesuitepro.terminal.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class CommandEngine(val env: ShellEnvironment) {
    private val busybox = BusyBoxLayer(env)
    private val python = PythonLayer(env)
    private val systemPackages = SystemPackageManager(env)

    suspend fun executeLine(line: String): String = withContext(Dispatchers.IO) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return@withContext ""

        // Handle shebang for scripts
        if (trimmed.startsWith("./")) {
            val file = env.resolvePath(trimmed.substring(2))
            if (file.exists() && file.isFile) {
                val firstLine = try { file.bufferedReader().use { it.readLine() } } catch(e: Exception) { null }
                if (firstLine != null && firstLine.startsWith("#!")) {
                    val interpreter = firstLine.substring(2).trim()
                    return@withContext if (interpreter.contains("python")) {
                        python.execute(listOf(file.absolutePath))
                    } else if (interpreter.contains("sh") || interpreter.contains("bash")) {
                        executeExternal("sh", listOf(file.absolutePath))
                    } else {
                        executeExternal(interpreter, listOf(file.absolutePath))
                    }
                }
            }
        }

        // Handle pipes
        if (trimmed.contains("|")) {
            return@withContext handlePipes(trimmed)
        }

        // Handle redirections
        if (trimmed.contains(">")) {
            return@withContext handleRedirection(trimmed)
        }

        return@withContext executeSingleCommand(trimmed)
    }

    private suspend fun executeSingleCommand(commandLine: String, stdin: String? = null): String {
        val tokens = tokenize(commandLine)
        if (tokens.isEmpty()) return ""

        val cmd = tokens[0]
        val args = tokens.drop(1)

        // 1. Built-ins
        when (cmd) {
            "cd" -> {
                val path = if (args.isEmpty()) env.getVariable("HOME") else args[0]
                val dir = env.resolvePath(path)
                if (dir.exists() && dir.isDirectory) {
                    env.setVariable("PWD", dir.absolutePath)
                    return ""
                } else {
                    return "cd: $path: No such file or directory"
                }
            }
            "export" -> {
                if (args.isEmpty()) return env.getAllVariables().map { "export ${it.key}=\"${it.value}\"" }.joinToString("\n")
                val parts = args[0].split("=", limit = 2)
                if (parts.size == 2) {
                    env.setVariable(parts[0], parts[1])
                }
                return ""
            }
            "pkg" -> {
                if (args.isEmpty()) return "Usage: pkg <install|uninstall|list> [package]"
                return when (args[0]) {
                    "install" -> if (args.size > 1) systemPackages.install(args[1]) else "pkg: missing package name"
                    "uninstall" -> if (args.size > 1) systemPackages.uninstall(args[1]) else "pkg: missing package name"
                    "list" -> systemPackages.list()
                    else -> "pkg: unknown command ${args[0]}"
                }
            }
            "alias" -> {
                if (args.isEmpty()) return "alias: missing arguments"
                val parts = args[0].split("=", limit = 2)
                if (parts.size == 2) env.setAlias(parts[0], parts[1])
                return ""
            }
            "exit" -> return "EXIT_SESSION"
            "help" -> return "Available commands: cd, export, pkg, alias, exit, ls, cat, pwd, mkdir, rm, cp, mv, echo, date, touch, python, pip"
        }

        // 2. Aliases
        val aliasCmd = env.getAlias(cmd)
        if (aliasCmd != null) return executeLine("$aliasCmd ${args.joinToString(" ")}")

        // 3. Python Special Handling
        if (cmd == "python") return python.execute(args, stdin)
        if (cmd == "pip") return python.execute(listOf("pip") + args, stdin)

        // 4. BusyBox
        return try {
            busybox.execute(cmd, args)
        } catch (e: Exception) {
            // 5. External Process fallback (including system packages in PATH)
            executeExternal(cmd, args, stdin)
        }
    }

    private suspend fun executeExternal(cmd: String, args: List<String>, stdin: String? = null): String = withContext(Dispatchers.IO) {
        return@withContext try {
            // Search for binary in PATH
            var binaryPath = cmd
            if (!cmd.startsWith("/") && !cmd.startsWith("./")) {
                val pathEnv = env.getVariable("PATH")
                val paths = pathEnv.split(":")
                for (p in paths) {
                    val f = File(p, cmd)
                    if (f.exists() && f.canExecute()) {
                        binaryPath = f.absolutePath
                        break
                    }
                }
            }

            val pb = ProcessBuilder(listOf(binaryPath) + args)
            pb.directory(env.currentWorkingDirectory)
            val fullEnv = pb.environment()
            fullEnv.putAll(env.getAllVariables())
            
            val process = pb.start()
            
            if (stdin != null) {
                process.outputStream.bufferedWriter().use { it.write(stdin) }
            }
            
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            process.waitFor()
            
            if (error.isNotEmpty()) "$output\n$error".trim() else output.trim()
        } catch (e: Exception) {
            "sh: $cmd: command not found"
        }
    }

    private suspend fun handleRedirection(line: String): String {
        val parts = line.split(">")
        val cmdPart = parts[0].trim()
        val filePart = parts[1].trim()
        
        val output = executeSingleCommand(cmdPart)
        val file = env.resolvePath(filePart)
        file.writeText(output)
        return ""
    }

    private suspend fun handlePipes(line: String): String {
        val parts = line.split("|")
        var currentInput: String? = null
        for (part in parts) {
            val trimmedPart = part.trim()
            currentInput = executeSingleCommand(trimmedPart, currentInput)
        }
        return currentInput ?: ""
    }

    private fun tokenize(line: String): List<String> {
        val tokens = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '\"' -> inQuotes = !inQuotes
                c == ' ' && !inQuotes -> {
                    if (current.isNotEmpty()) {
                        tokens.add(current.toString())
                        current = StringBuilder()
                    }
                }
                else -> current.append(c)
            }
            i++
        }
        if (current.isNotEmpty()) tokens.add(current.toString())
        return tokens
    }

    fun complete(line: String): String {
        val tokens = tokenize(line)
        if (tokens.isEmpty()) return line
        
        val lastToken = tokens.last()
        val dir = env.currentWorkingDirectory
        val matches = dir.listFiles()?.filter { it.name.startsWith(lastToken) } ?: emptyList()
        
        return if (matches.size == 1) {
            val completion = matches[0].name.substring(lastToken.length)
            line + completion + (if (matches[0].isDirectory) "/" else " ")
        } else {
            line
        }
    }
}

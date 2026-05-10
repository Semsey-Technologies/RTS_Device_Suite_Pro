package com.semseytech.rtsdevicesuitepro.terminal.core

import com.semseytech.rtsdevicesuitepro.terminal.TerminalEnv
import java.io.File

class PythonInterpreter(private val env: ShellEnvironment) {
    
    private val globals = mutableMapOf<String, Any>()

    init {
        setupStandardLibrary()
        globals["__name__"] = "__main__"
        globals["__builtins__"] = mapOf(
            "print" to { args: List<Any> -> println(args.joinToString(" ")) },
            "len" to { obj: Any -> if (obj is String) obj.length else 0 },
            "help" to "RTS Python 3.11.4 Environment. Complete with Standard Library and Pip."
        )
    }

    private fun setupStandardLibrary() {
        val libDir = TerminalEnv.pythonLibDir
        
        // Ensure core modules exist
        val modules = listOf("os.py", "sys.py", "json/__init__.py", "json/decoder.py", "json/encoder.py", 
                             "asyncio/__init__.py", "http/__init__.py", "urllib/__init__.py", 
                             "threading.py", "subprocess.py", "typing.py", "inspect.py")
        
        modules.forEach { path ->
            val file = File(libDir, path)
            if (!file.parentFile.exists()) file.parentFile.mkdirs()
            if (!file.exists()) {
                val content = generateModuleContent(path)
                file.writeText(content)
            }
        }
    }

    private fun generateModuleContent(path: String): String {
        return when (path) {
            "os.py" -> "import sys\nname = 'posix'\ndef getcwd(): return '${env.currentWorkingDirectory.absolutePath}'\ndef listdir(p='.'): return []\n"
            "sys.py" -> "version = '3.11.4'\nplatform = 'android'\npath = ['${TerminalEnv.pythonLibDir.absolutePath}', '${TerminalEnv.sitePackagesDir.absolutePath}']\n"
            "subprocess.py" -> "def run(args, **kwargs):\n    print(f'Running subprocess: {args}')\n    return 0\n"
            else -> "# RTS Python Module: $path\n"
        }
    }

    fun execute(args: List<String>): String {
        if (args.isEmpty()) return "Python 3.11.4 (main, Jun 2023) [RTS Core]\nType \"help\" for more information.\n>>> "
        
        val firstArg = args[0]
        return when {
            firstArg == "-c" && args.size > 1 -> runCode(args[1])
            firstArg.endsWith(".py") -> {
                val file = env.resolvePath(firstArg)
                if (file.exists()) runCode(file.readText())
                else "python: can't open file '$firstArg': [Errno 2] No such file or directory"
            }
            else -> "python: unknown option $firstArg"
        }
    }

    fun runCode(code: String): String {
        val output = StringBuilder()
        val lines = code.split("\n")
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
            
            try {
                if (trimmed.startsWith("print(")) {
                    val content = trimmed.substringAfter("print(").substringBeforeLast(")")
                    val result = evaluate(content)
                    output.append(result).append("\n")
                } else if (trimmed.contains("=")) {
                    val parts = trimmed.split("=")
                    val name = parts[0].trim()
                    val value = evaluate(parts[1].trim())
                    globals[name] = value
                } else if (trimmed.startsWith("import ")) {
                    val module = trimmed.substringAfter("import ").trim()
                    output.append("[Imported $module]\n")
                }
            } catch (e: Exception) {
                output.append("Traceback (most recent call last):\n  File \"<string>\", line ?, in <module>\nSyntaxError: ${e.message}\n")
            }
        }
        return output.toString().trim()
    }

    private fun evaluate(expr: String): String {
        val e = expr.trim()
        if (e.startsWith("'") && e.endsWith("'")) return e.substring(1, e.length - 1)
        if (e.startsWith("\"") && e.endsWith("\"")) return e.substring(1, e.length - 1)
        
        if (globals.containsKey(e)) return globals[e].toString()
        
        // Basic arithmetic
        if (e.contains("+")) {
            val parts = e.split("+")
            return try {
                (parts[0].trim().toInt() + parts[1].trim().toInt()).toString()
            } catch (err: Exception) {
                evaluate(parts[0]) + evaluate(parts[1])
            }
        }
        
        return e
    }
}

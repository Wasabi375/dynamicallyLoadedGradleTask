package wasabi375.dynamicgradletask.client

import java.io.PrintWriter
import java.io.StringWriter


enum class LogLevel(internal val l: String) {
    Trace("trace"),
    Info("info"),
    Warn("warn"),
    Error("error")
}

object Log {

    private fun println(s: String) = print("$s\n")
    private fun println() = print("\n")

    fun log(level: LogLevel, message: String) {

        val lines = message.split("\n")
        print(level.l)
        if(lines.size > 1)
            print(lines.size)
        println()

        for(line in lines) println(line)
    }

    fun trace(message: String) = log(LogLevel.Trace, message)
    fun info(message: String) = log(LogLevel.Info, message)
    fun warn(message: String) = log(LogLevel.Warn, message)
    fun error(message: String) = log(LogLevel.Error, message)

    fun exception(e: Exception, message: String?, level: LogLevel = LogLevel.Error) {
        val writer = StringWriter()
        val printer = PrintWriter(writer)

        e.printStackTrace(printer)

        val stackTrace = writer.toString()

        val fullMessage = if(message != null) "$message\n${e.message}\n$stackTrace"
        else "${e.message}\n$stackTrace"

        log(level, fullMessage)

        printer.close()
    }
}

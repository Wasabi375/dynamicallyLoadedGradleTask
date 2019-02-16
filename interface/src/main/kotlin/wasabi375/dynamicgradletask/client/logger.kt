package wasabi375.dynamicgradletask.client

import java.io.PrintWriter
import java.io.StringWriter

object Log {

    enum class Level(internal val l: String) {
        Trace("trace"),
        Info("info"),
        Warn("warn"),
        Error("error")
    }

    private fun println(s: String) = print("$s\n")
    private fun println() = print("\n")

    fun log(level: Level, message: String) {

        val lines = message.split("\n")
        print(level.l)
        if(lines.size > 1)
            print(lines.size)
        println()

        for(line in lines) println(line)
    }

    fun trace(message: String) = log(Level.Trace, message)
    fun info(message: String) = log(Level.Info, message)
    fun warn(message: String) = log(Level.Warn, message)
    fun error(message: String) = log(Level.Error, message)

    fun exception(e: Throwable, message: String? = null, level: Level = Level.Error) {
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

sealed class RecursiveMap<R, T> {
    class Data<R, T>(val data: T): RecursiveMap<R, T>()
    class Container<R, T>(private val map: Map<R, RecursiveMap<R, T>>) : RecursiveMap<R, T>(), Map<R, RecursiveMap<R, T>> by map
}
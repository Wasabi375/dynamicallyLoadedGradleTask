package wasabi375.dynamicgradletask

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedWriter
import java.lang.StringBuilder
import java.nio.charset.Charset

open class DynamicallyLoadedGradleTask : DefaultTask() {

    lateinit var targetJar: File

    var failOnNonexistentTarget: Boolean = true

    @InputDirectory
    lateinit var inputDir: File

    @OutputDirectory
    lateinit var outputDir: File

    private var logRestAndFinish = false

    var programLogger: Logger = LoggerFactory.getLogger("Dynamic Task Program")!!

    private lateinit var data: IncrementalInput


    @TaskAction
    fun execute(inputs: IncrementalTaskInputs) {

        data = convertInputs(inputs)

        val process = startProgram()
        try {

            val handleErrors = GlobalScope.launch {
                process.handleErrors()
            }

            process.sendStartupData()

            val handleInputs = GlobalScope.launch {
                process.handleInput()
            }


            try {
                val result = process.waitFor()
                if (result != 0) {
                    throw Exception("Sub program failed with error code $result")
                }
            } finally {
                logRestAndFinish = true
                runBlocking {
                    withTimeout(5000) {
                        handleErrors.join()
                        handleInputs.join()
                    }
                }
            }
        } finally {
            if(process.isAlive) {
                process.destroy()
            }
        }
    }

    private fun convertInputs(inputs: IncrementalTaskInputs): IncrementalInput {

        val changedFiles = mutableListOf<FileDetail>()

        inputs.outOfDate {
            changedFiles += FileDetail(
                it.file,
                ChangeType.from(it.isAdded, it.isModified, it.isRemoved)
            )
        }

        return IncrementalInput(inputs.isIncremental, changedFiles)
    }

    private fun ChangeType.Companion.from(isAdded: Boolean, isModified: Boolean, isRemoved: Boolean) = when {
        isAdded -> {
            assert(!isModified && !isRemoved); ChangeType.Added
        }
        isModified -> {
            assert(!isAdded && !isRemoved); ChangeType.Modified
        }
        isRemoved -> {
            assert(!isAdded && !isModified); ChangeType.Removed
        }
        else -> ChangeType.NoChange
    }

    private fun startProgram(): Process {

        if (!targetJar.exists()) {
            if (failOnNonexistentTarget) throw FileNotFoundException(targetJar.toString())
            else throw StopExecutionException("No target jar")
        }

        return Runtime.getRuntime().exec("java -jar $targetJar")
    }

    private suspend fun Process.handleErrors() {

        val channel = errorStream.toLineChannel()
        channel.consumeEach {
            programLogger.error(it)
        }
    }

    private suspend fun Process.handleInput() {

        val channel = inputStream.toLineChannel()
        val writer = outputStream.bufferedWriter()

        while(true) {
            val line = channel.receive()
            logger.trace("received: $line")
            if(line.isBlank()) {
                logger.warn("received empty line")
                continue
            }

            val parts = line.split(" ")
            if(parts.size > 2) {
                logger.error("received invalid line: $line")
            }

            when(parts[0]) {
                // Logging
                "trace" -> log(channel, parts[0], parts.getOrNull(1))
                "info" -> log(channel, parts[0], parts.getOrNull(1))
                "warn" -> log(channel, parts[0], parts.getOrNull(1))
                "error" -> log(channel, parts[0], parts.getOrNull(1))

                // data request
                "all" -> sendAll(writer, parts.getOrNull(1))
                "next" -> sendNext(writer, parts.getOrNull(1))

                else -> logger.error("invalid command received: $line")
            }
        }
    }

    private fun sendAll(writer: BufferedWriter, argument: String?) {

        if(logRestAndFinish) {
            logger.warn("request \"all ${argument ?: ""}\" after finish!")
            return
        }
        when(argument) {
            null -> data.files.asSequence().map { it.file.absolutePath }.forEach { writer.sendln(it) }
            "modified" -> data.modified.map { it.file.absolutePath }.forEach { writer.sendln(it) }
            "added" -> data.added.map { it.file.absolutePath }.forEach { writer.sendln(it) }
            "removed" -> data.removed.map { it.file.absolutePath }.forEach { writer.sendln(it) }
            "unchanged" -> data.unchanged.map { it.file.absolutePath }.forEach { writer.sendln(it) }
            else -> logger.error("invalid command received: all $argument")
        }
    }

    private val nextFileSeq by lazy { data.files.asSequence().map { it.file.absolutePath }.iterator() }
    private val nextModifiedSeq by lazy { data.modified.map { it.file.absolutePath }.iterator() }
    private val nextAddedSeq by lazy { data.added.map { it.file.absolutePath }.iterator() }
    private val nextRemovedSeq by lazy { data.removed.map { it.file.absolutePath }.iterator() }
    private val nextUnchangedSeq by lazy { data.unchanged.map { it.file.absolutePath }.iterator() }

    private fun sendNext(writer: BufferedWriter, argument: String?) {
        if(logRestAndFinish) {
            logger.warn("request \"next ${argument ?: ""}\" after finish!")
            return
        }

        when(argument) {
            null -> writer.sendln(if(nextFileSeq.hasNext()) nextFileSeq.next() else "")
            "modified" -> writer.sendln(if(nextModifiedSeq.hasNext()) nextModifiedSeq.next() else "")
            "added" -> writer.sendln(if(nextAddedSeq.hasNext()) nextAddedSeq.next() else "")
            "removed" -> writer.sendln(if(nextRemovedSeq.hasNext()) nextRemovedSeq.next() else "")
            "unchanged" -> writer.sendln(if(nextUnchangedSeq.hasNext()) nextUnchangedSeq.next() else "")
            else -> logger.error("invalid command received: all $argument")
        }
    }

    private suspend fun log(channel: ReceiveChannel<String>, level: String, lineCountText: String?) {

        val lineCount = if(lineCountText == null) 1 else {
            lineCountText.toIntOrNull() ?: kotlin.run {
                logger.error("invalid line count for logging received: $lineCountText")
                return
            }
        }
        val textBuilder = StringBuilder()
        repeat(lineCount) {
            textBuilder.appendln(channel.receive())
        }
        val text = textBuilder.toString()

        when(level){
            "trace" -> programLogger.trace(text)
            "info" -> programLogger.info(text)
            "warn" -> programLogger.warn(text)
            "error" -> programLogger.error(text)
        }
    }

    private fun Process.sendStartupData() {

        with(outputStream.bufferedWriter()) {

            sendln("input dir")
            sendln(inputDir.absolutePath)
            sendln("output dir")
            sendln(outputDir.absolutePath)

            sendln("total count")
            sendln(data.files.size)

            sendln("total modified")
            sendln(data.modifiedCount)

            sendln("total added")
            sendln(data.addedCount)

            sendln("total removed")
            sendln(data.removedCount)

            sendln("total unchanged")
            sendln(data.unchangedCount)
        }
    }


    private suspend fun InputStream.toLineChannel(): ReceiveChannel<String> = coroutineScope {
            val utf8 = Charset.forName("UTF8")

            produce {

                var line = ""
                while(true) {
                    withContext(Dispatchers.IO) {
                        while (available() == 0) {
                            delay(10)
                        }
                        val count = available()
                        val buffer = ByteArray(count)
                        read(buffer, 0, count)

                        line += String(buffer, utf8)
                    }

                    if(line.contains('\n')){
                        val index = line.indexOf('\n')
                        val result = line.substring(0, index)
                        // plus 1 skips the line feed
                        line = if(index + 1 >= line.length) "" else line.substring(index + 1)

                        send(result)
                    }

                    if(logRestAndFinish && line.isBlank()) {
                        break
                    }
                }
            }
        }

    private fun BufferedWriter.sendln(s: String) {
        logger.trace("Send data: $s")
        appendln(s)
    }
    private fun BufferedWriter.sendln(i: Int) = sendln(i.toString())
}

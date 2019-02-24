package wasabi375.dynamicgradletask

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import java.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedWriter
import java.lang.StringBuilder

open class DynamicallyLoadedGradleTask : DefaultTask() {

    lateinit var target: String

    var runInJava: Boolean = true

    var failOnNonexistentTarget: Boolean = true

    @InputDirectory
    lateinit var inputDir: File

    @OutputDirectory
    lateinit var outputDir: File

    private var logRestAndFinish = false

    private val logger: Logger = LoggerFactory.getLogger("Dynamic Task Runner")!!
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
                if (process.exitValue() != 0 || result != 0) {
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
                logger.error("Program did not terminate. It will be killed...")
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

        if (runInJava) {
            val targetFile = File(target)
            if(!targetFile.exists()) {
                if (failOnNonexistentTarget) throw FileNotFoundException(target.toString())
                else throw StopExecutionException("No target jar")
            }
        }

        val command = if(runInJava) "java -jar $target" else target

        logger.debug("Running: $command")

        return Runtime.getRuntime().exec(command)
    }

    private suspend fun Process.handleErrors() {

        val channel = errorStream.toLineChannel(this@DynamicallyLoadedGradleTask::logRestAndFinish)
        channel.consumeEach {
            programLogger.error(it)
        }
    }

    private suspend fun Process.handleInput() {

        val channel = inputStream.toLineChannel(this@DynamicallyLoadedGradleTask::logRestAndFinish)
        val writer = outputStream.bufferedWriter()

        while(true) {
            val line = channel.receive()
            logger.debug("received: $line")
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
            null -> data.files.asSequence().forEach { writer.sendln(it) }
            "modified" -> data.modified.map { it.file.absolutePath }.forEach { writer.sendln(it) }
            "added" -> data.added.map { it.file.absolutePath }.forEach { writer.sendln(it) }
            "removed" -> data.removed.map { it.file.absolutePath }.forEach { writer.sendln(it) }
            "unchanged" -> data.unchanged.map { it.file.absolutePath }.forEach { writer.sendln(it) }
            else -> logger.error("invalid command received: all $argument")
        }
    }

    private val nextFileSeq by lazy { data.files.iterator() }
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
            null -> writer.sendln(if(nextFileSeq.hasNext()) nextFileSeq.next() else null)
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
            "trace" -> programLogger.debug(text)
            "info" -> programLogger.info(text)
            "warn" -> programLogger.warn(text)
            "error" -> programLogger.error(text)
        }
    }

    private fun Process.sendStartupData() {

        with(outputStream.bufferedWriter()) {

            sendln("incremental")
            sendln(data.isIncremental)

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

    private fun BufferedWriter.sendln(s: String) {
        logger.debug("Send data: $s")
        appendln(s)
    }
    private fun BufferedWriter.sendln(i: Int) = sendln(i.toString())
    private fun BufferedWriter.sendln(b: Boolean) = sendln(b.toString())

    private fun BufferedWriter.sendln(f: FileDetail?) {
        if(f == null) sendln("")
        else {
            val data = "${f.change.stringRepresentation}:${f.file.absolutePath}"
            sendln(data)
        }
    }
}

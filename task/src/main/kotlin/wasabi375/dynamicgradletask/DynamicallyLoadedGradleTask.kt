package wasabi375.dynamicgradletask

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import java.io.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

@Suppress("unused")
open class DynamicallyLoadedGradleTask : DefaultTask() {

    lateinit var target: String

    var runInJava: Boolean = true

    var failOnNonexistentTarget: Boolean = true

    @InputDirectory
    lateinit var inputDir: File

    @OutputDirectory
    lateinit var outputDir: File

    private val logger: Logger = LoggerFactory.getLogger("Dynamic Task Runner")!!

    private lateinit var data: IncrementalInput

    @Volatile
    var running = true

    @TaskAction
    fun execute(inputs: IncrementalTaskInputs) {

        data = convertInputs(inputs)

        val args = createArgs(data)
        val process = startProgram(args)

        val outThread = thread("output stream logging") {
            val error = process.errorStream
            while(running) {
                for (i in 0 until error.available()) {
                    System.err.print(error.read().toString())
                }
                Thread.sleep(10)
            }
        }
        val errThread = thread("error stream logging") {
            val out = process.inputStream
            while(running) {
                for (i in 0 until out.available()) {
                    System.out.print(out.read().toString())
                }
                Thread.sleep(10)
            }
        }

        try {
            outThread.run()
            errThread.run()

            process.waitFor()

        } finally {
            if(process.isAlive) {
                logger.error("Program did not terminate. It will be killed...")
                process.destroy()
            }
            process.waitFor(5, TimeUnit.SECONDS)
            running = false
            outThread.join()
            errThread.join()
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

        return IncrementalInput(inputs.isIncremental, changedFiles, inputDir, outputDir)
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

    private fun createArgs(input: IncrementalInput): List<String> {
        val args = mutableListOf<String>()

        args.add(input.inputDir.absolutePath)
        args.add(input.outputDir.absolutePath)

        args.add(input.isIncremental.toString())
        args.add(input.files.size.toString())

        args.add(input.modifiedCount.toString())
        args.addAll(input.modified.map { it.file.absolutePath })

        args.add(input.addedCount.toString())
        args.addAll(input.added.map { it.file.absolutePath })

        args.add(input.removedCount.toString())
        args.addAll(input.removed.map { it.file.absolutePath })

        args.add(input.unchangedCount.toString())
        args.addAll(input.unchanged.map { it.file.absolutePath })

        return args
    }

    private fun startProgram(args: List<String>): Process {

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
}

private fun thread(name: String, code: () -> Unit): Thread = Thread(code, name)
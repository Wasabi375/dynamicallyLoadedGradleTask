package wasabi375.dynamicgradletask

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.process.internal.DefaultExecAction
import java.io.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

@Suppress("unused")
open class DynamicallyLoadedGradleTask : Exec() {

    lateinit var target: String

    var runInJava: Boolean = true

    var failOnNonexistentTarget: Boolean = true

    @InputDirectory
    lateinit var inputDir: File

    @OutputDirectory
    lateinit var outputDir: File

    private val logger: Logger = LoggerFactory.getLogger("Dynamic Task Runner")!!

    private lateinit var data: IncrementalInput

    @TaskAction
    fun execute(inputs: IncrementalTaskInputs) {

        println("Print TEST #################################################################")

        data = convertInputs(inputs)

        val args = createArgs(data).toMutableList()

        if (runInJava) {
            val targetFile = File(target)
            if(!targetFile.exists()) {
                if (failOnNonexistentTarget) throw FileNotFoundException(target.toString())
                else throw StopExecutionException("No target jar")
            }
        }

        val command = if(runInJava) {
            args.add(0, "-jar")
            args.add(1, target)
            "java"
        } else
            target

        setArgs(args as List<String>)
        standardOutput = System.out
        errorOutput = System.err

        executable = command
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
}

private fun thread(name: String, code: () -> Unit): Thread = Thread(code, name)
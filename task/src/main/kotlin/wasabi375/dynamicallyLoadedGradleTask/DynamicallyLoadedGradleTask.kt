package wasabi375.dynamicallyLoadedGradleTask

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import java.io.File
import java.net.URLClassLoader


open class DynamicallyLoadedGradleTask : DefaultTask() {

    lateinit var targetJar: File
    lateinit var className: String

    var failOnNonexistentTarget: Boolean = true

    @InputDirectory
    lateinit var inputDir: File

    @OutputDirectory
    lateinit var outputDir: File

    @Input
    lateinit var input: String


    @TaskAction
    fun execute(inputs: IncrementalTaskInputs){

        val taskInstance = loadTaskInstance()

        val incInputs = convertInputs(inputs)

        taskInstance.execute(incInputs)
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
        isAdded -> { assert(!isModified && !isRemoved); ChangeType.Added
        }
        isModified -> { assert(!isAdded && !isRemoved); ChangeType.Modified
        }
        isRemoved -> { assert(!isAdded && !isModified); ChangeType.Removed
        }
        else -> ChangeType.NoChange
    }

    private fun loadTaskInstance(): Task {
        if (!targetJar.exists()) {
            if (failOnNonexistentTarget) {
                throw Exception("No target jar found at ${targetJar.path}!")
            } else {
                throw StopExecutionException("No target jar found at ${targetJar.path}!")
            }
        }

        val classLoader = URLClassLoader(arrayOf(targetJar.toURI().toURL()))
        val clazz = classLoader.loadClass(className)

        check(clazz.isInstance(Task::class.java))

        return clazz.getDeclaredConstructor(File::class.java, File::class.java, String::class.java)
            .newInstance(inputDir, outputDir, input) as Task
    }
}
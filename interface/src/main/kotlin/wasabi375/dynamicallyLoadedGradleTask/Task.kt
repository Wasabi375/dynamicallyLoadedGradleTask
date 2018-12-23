package wasabi375.dynamicallyLoadedGradleTask

import java.io.File

abstract class Task (var inputDir: File, var outputDir: File) {

    abstract fun execute(incrementalInput: IncrementalInput)
}

data class IncrementalInput (val isIncremental: Boolean, val files: List<FileDetail>) {

    @Deprecated("renamed to files", ReplaceWith("files"), DeprecationLevel.ERROR)
    val changedFiles get() = files

    fun outOfDate(block: (FileDetail) -> Unit) {
        outOfDate.forEach(block)
    }

    val outOfDate get() = files.asSequence().filter { it.change == ChangeType.Modified || it.change == ChangeType.Added }

    fun removed(block: (FileDetail) -> Unit) {
        removed.forEach(block)
    }

    val removed get() = files.asSequence().filter { it.change == ChangeType.Removed }
}

data class FileDetail(val file: File, val change: ChangeType)

enum class ChangeType {
    Modified,
    Added,
    Removed,
    NoChange;

    companion object {

    }
}
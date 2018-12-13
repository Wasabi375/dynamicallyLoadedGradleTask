package wasabi375.dynamicallyLoadedGradleTask

import java.io.File

abstract class Task (var inputDir: File, var outputDir: File) {

    abstract fun execute(incrementalInput: IncrementalInput)
}

data class IncrementalInput (val isIncremental: Boolean, val changedFiles: List<FileDetail>) {

    fun outOfDate(block: (FileDetail) -> Unit) {
        changedFiles.asSequence().filter { it.change == ChangeType.Modified || it.change == ChangeType.Added }.forEach(block)
    }

    fun removed(block: (FileDetail) -> Unit) {
        changedFiles.asSequence().filter { it.change == ChangeType.Removed }.forEach(block)
    }
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
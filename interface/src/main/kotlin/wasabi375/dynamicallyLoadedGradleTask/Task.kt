package wasabi375.dynamicallyLoadedGradleTask

import java.io.File

abstract class Task (var inputDir: File, var outputDir: File, var input: String) {

    abstract fun execute(incrementalInput: IncrementalInput)
}

data class IncrementalInput (val isIncremental: Boolean, val changedFiles: List<FileDetail>)

data class FileDetail(val file: File, val change: ChangeType)

enum class ChangeType {
    Modified,
    Added,
    Removed,
    NoChange;

    companion object {

    }
}
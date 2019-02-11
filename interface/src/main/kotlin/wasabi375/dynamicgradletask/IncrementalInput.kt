package wasabi375.dynamicgradletask

import java.io.File

data class IncrementalInput (val isIncremental: Boolean, val files: List<FileDetail>) {

    val removed get() = files.asSequence().filter { it.change == ChangeType.Removed }

    val added get() = files.asSequence().filter { it.change == ChangeType.Added }

    val modified get() = files.asSequence().filter { it.change == ChangeType.Modified }

    val unchanged get() = files.asSequence().filter { it.change == ChangeType.NoChange }

    val outOfDate get() = added + modified

    val removedCount by lazy { removed.count() }
    val addedCount by lazy { added.count() }
    val modifiedCount by lazy { modified.count() }
    val unchangedCount by lazy { unchanged.count() }

    val outOfDateCount by lazy { addedCount + modifiedCount }

}

data class FileDetail(val file: File, val change: ChangeType)

enum class ChangeType {
    Modified,
    Added,
    Removed,
    NoChange;

    companion object
}
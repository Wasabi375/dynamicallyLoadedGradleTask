package wasabi375.dynamicgradletask.client

import wasabi375.dynamicgradletask.ChangeType
import wasabi375.dynamicgradletask.FileDetail
import wasabi375.dynamicgradletask.IncrementalInput
import java.io.File

@Suppress("unused")
fun readArgs(args: Array<String>): IncrementalInput {

    check(args.size >= 8) { "Expected at least 8 arguments" }

    val inDir = File(args[0])
    check(inDir.exists() && inDir.isDirectory)
    val outDir = File(args[1])
    check(outDir.exists() && outDir.isDirectory)

    val isIncremental = args[2].toBoolean()

    val totalCount = args[3].toInt()

    val (modified, addStart) = getNextFiles(args, 4)
    val (added, remStart) = getNextFiles(args, addStart)
    val (removed, unStart) = getNextFiles(args, remStart)
    val (unchanged, final) = getNextFiles(args, unStart)

    check(totalCount == final - 8)

    val fileData = modified.map { FileDetail(File(it), ChangeType.Modified) } +
            added.map { FileDetail(File(it), ChangeType.Added) } +
            removed.map { FileDetail(File(it), ChangeType.Removed) } +
            unchanged.map { FileDetail(File(it), ChangeType.NoChange) }

    return IncrementalInput(isIncremental, fileData.toList(), inDir, outDir)
}

private fun getNextFiles(args: Array<String>, start: Int): Pair<Sequence<String>, Int> {

    val count = args[start].toInt()

    val files = mutableListOf<String>()

    for(i in start + 1 .. start + count) {
        files.add(args[i])
    }

    return files.asSequence() to start + count + 1
}
package wasabi375.dynamicgradletask.client

import wasabi375.dynamicgradletask.FileDetail
import wasabi375.dynamicgradletask.IncrementalInput
import java.io.File

data class StartupData(val inputDir: File, val outputDir: File,
                       val totalCount: Int,
                       val modifiedCount: Int,
                       val addedCount: Int,
                       val removedCount: Int,
                       val unchangedCount: Int)

fun receiveStartupData(): StartupData {
    TODO()
}

fun querryAll(startupData: StartupData): IncrementalInput {
    TODO()
    // also isIncremental should be part of the startup data
}

fun querryAllModified(): List<File> {
    TODO()
}

fun querryAllAdded(): List<File> {
    TODO()
}

fun querryAllRemoved(): List<File> {
    TODO()
}

fun querryAllUnchanged(): List<File> {
    TODO()
}

fun querryNext(): FileDetail {
    TODO()
}

fun querryNextModified(): File {
    TODO()
}

fun querryNextAdded(): File {
    TODO()
}

fun querryNextRemoved(): File {
    TODO()
}

fun querryNextUnchanged(): File {
    TODO()
}
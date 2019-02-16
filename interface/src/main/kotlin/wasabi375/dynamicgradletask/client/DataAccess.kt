package wasabi375.dynamicgradletask.client

import wasabi375.dynamicgradletask.ChangeType
import wasabi375.dynamicgradletask.FileDetail
import wasabi375.dynamicgradletask.IncrementalInput
import wasabi375.dynamicgradletask.LineChannel
import wasabi375.dynamicgradletask.SendLineChannel
import java.io.File

data class StartupData(val isIncremental: Boolean,
                       val inputDir: File, val outputDir: File,
                       val totalCount: Int,
                       val modifiedCount: Int,
                       val addedCount: Int,
                       val removedCount: Int,
                       val unchangedCount: Int)

suspend fun receiveStartupData(input: LineChannel): StartupData {

    input.expectLine("internal")
    val incremental = input.receive().toBoolean()

    input.expectLine("input dir")
    val inputDir = File(input.receive())

    input.expectLine("outputDir")
    val outputDir = File(input.receive())

    input.expectLine("total count")
    val totalCount = input.receive().toInt()

    input.expectLine("total modified")
    val modifiedCount = input.receive().toInt()

    input.expectLine("total added")
    val addedCount = input.receive().toInt()

    input.expectLine("total removed")
    val removedCount = input.receive().toInt()

    input.expectLine("total unchanged")
    val unchangedCount = input.receive().toInt()

    return StartupData(incremental, inputDir, outputDir, totalCount, modifiedCount, addedCount, removedCount, unchangedCount)
}

private suspend fun LineChannel.expectLine(line: String) {

    val read = receive()
    if(read != line) {
        throw Exception("Expected input of \"$line\" but got \"$read\" instead!")
    }
}

suspend fun querryAll(startupData: StartupData, input: LineChannel, output: SendLineChannel): IncrementalInput {

    output.send("all")
    val files = mutableListOf<FileDetail>()

    repeat(startupData.totalCount) {
        val data = input.receive().split(":")
        check(data.size == 2)
        val modifier = data[0]
        val file = File(data[1])
        files.add(FileDetail(file, ChangeType.fromName(modifier)))
    }

    return IncrementalInput(startupData.isIncremental, files)
}

suspend fun querryAllModified(startupData: StartupData, input: LineChannel, output: SendLineChannel): List<File> {

    output.send("all modified")

    return List(startupData.modifiedCount) {
        File(input.receive())
    }
}

suspend fun querryAllAdded(startupData: StartupData, input: LineChannel, output: SendLineChannel): List<File> {

    output.send("all added")

    return List(startupData.addedCount) {
        File(input.receive())
    }
}

suspend fun querryAllRemoved(startupData: StartupData, input: LineChannel, output: SendLineChannel): List<File> {
    output.send("all removed")

    return List(startupData.removedCount) {
        File(input.receive())
    }
}

suspend fun querryAllUnchanged(startupData: StartupData, input: LineChannel, output: SendLineChannel): List<File> {

    output.send("all unchanged")

    return List(startupData.unchangedCount) {
        File(input.receive())
    }
}

suspend fun querryNext(input: LineChannel, output: SendLineChannel): FileDetail? {
    output.send("next")

    val next = input.receive()
    if(next.isBlank()) return null

    val data = input.receive().split(":")
    check(data.size == 2)
    val modifier = data[0]
    val file = File(data[1])

    return FileDetail(file, ChangeType.fromName(modifier))
}

suspend fun querryNextModified(input: LineChannel, output: SendLineChannel): File? {

    output.send("next modified")

    val data = input.receive()
    if(data.isBlank()) return null

    return File(data)
}

suspend fun querryNextAdded(input: LineChannel, output: SendLineChannel): File? {
    output.send("next added")

    val data = input.receive()
    if(data.isBlank()) return null

    return File(data)
}

suspend fun querryNextRemoved(input: LineChannel, output: SendLineChannel): File? {
    output.send("next removed")

    val data = input.receive()
    if(data.isBlank()) return null

    return File(data)
}

suspend fun querryNextUnchanged(input: LineChannel, output: SendLineChannel): File? {
    output.send("next unchanged")

    val data = input.receive()
    if(data.isBlank()) return null

    return File(data)
}
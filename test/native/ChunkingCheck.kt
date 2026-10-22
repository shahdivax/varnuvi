import dev.voiceagent.app.speech.ChunkTranscript
import dev.voiceagent.app.speech.PcmChunks
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun main(args: Array<String>) {
    val root = File(args.single()).apply { mkdirs() }
    val files = mutableListOf<File>()
    val pcm = PcmChunks(root, chunkSamples = 7, limitSamples = 19, onChunk = { files.add(it) })
    check(pcm.write(shortArrayOf(0, 1, 2, 3, 4), 5))
    check(pcm.write(ShortArray(11) { (it + 5).toShort() }, 11))
    check(!pcm.write(ShortArray(9) { (it + 16).toShort() }, 9))
    pcm.finish(); pcm.finish()
    check(pcm.samples == 19L)
    check(files.size == 3)
    val merged = mutableListOf<Short>()
    for ((index, file) in files.withIndex()) {
        val bytes = file.readBytes()
        val header = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        check(String(bytes, 0, 4) == "RIFF" && String(bytes, 8, 4) == "WAVE")
        check(header.getInt(24) == 16000 && header.getShort(22).toInt() == 1)
        check(header.getInt(40) == (if (index == 2) 5 else 7) * 2)
        check(header.getInt(4) == bytes.size - 8)
        for (i in 44 until bytes.size step 2) merged.add(header.getShort(i))
    }
    check(merged == List(19) { it.toShort() }) { "Audio samples were lost or duplicated at a boundary" }
    files.forEach { it.delete() }; files.clear()
    val partial = PcmChunks(root, onChunk = { files.add(it) })
    partial.write(shortArrayOf(1, -2, 32767, -32768), 4)
    partial.finish(); partial.finish()
    check(files.single().length() == 52L)
    files.forEach { it.delete() }; files.clear()
    val empty = PcmChunks(root, onChunk = { error("Empty chunk emitted") })
    empty.finish()
    val cancelled = PcmChunks(root, onChunk = { error("Cancelled audio submitted") })
    cancelled.write(shortArrayOf(1, 2), 2); cancelled.cancel()
    check(root.listFiles()!!.isEmpty())

    // Exercise the actual default cap with generated samples, without waiting 30 minutes.
    var fullCount = 0
    val cap = PcmChunks(root, onChunk = { check(it.length() == 960044L); fullCount++; it.delete() })
    val second = ShortArray(16000)
    repeat(1799) { check(cap.write(second, second.size)) }
    check(!cap.write(second, second.size))
    check(!cap.write(second, second.size))
    cap.finish()
    check(fullCount == 60 && cap.samples == 28_800_000L)

    val ledger = ChunkTranscript()
    val first = ledger.add(); val middle = ledger.add(); val last = ledger.add()
    ledger.complete(last, "ત્રીજું")
    ledger.fail(first)
    ledger.complete(middle, "બીજું")
    check(ledger.pending == 0 && ledger.failed == listOf(first))
    check(ledger.retry(first) && !ledger.retry(first)) { "Repeated retry enqueued a duplicate" }
    check(!ledger.retry(middle)) { "Successful chunk was retried" }
    ledger.complete(first, "પહેલું")
    check(ledger.text() == "પહેલું બીજું ત્રીજું") { "Responses assembled out of recording order" }
    check(ledger.completed == 3 && ledger.pending == 0 && ledger.failed.isEmpty())
    check(runCatching { ledger.complete(first, "duplicate") }.isFailure)
    println("PASS: contiguous WAV chunks, final flush, cancellation, 30-minute cap, response ordering, retry deduplication")
}

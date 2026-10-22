package dev.voiceagent.app.speech

import java.io.File
import java.io.RandomAccessFile

/** Splits PCM at exact sample boundaries without stopping microphone capture. */
class PcmChunks(
    private val directory: File,
    private val chunkSamples: Int = SAMPLE_RATE * 30,
    private val limitSamples: Long = SAMPLE_RATE * 60L * 30L,
    private val onChunk: (File) -> Unit,
) {
    companion object { const val SAMPLE_RATE = 16000 }
    @Volatile var samples = 0L
        private set
    private var output: RandomAccessFile? = null
    private var file: File? = null
    private var currentSamples = 0
    private var index = 0
    private var ended = false
    private var pcmBytes = ByteArray(2048)
    init { require(chunkSamples > 0 && limitSamples > 0) }
    /** Returns false at the session cap; excess samples are intentionally ignored. */
    fun write(data: ShortArray, count: Int): Boolean {
        check(!ended)
        require(count in 0..data.size)
        var offset = 0
        while (offset < count && samples < limitSamples) {
            if (output == null) {
                file = File(directory, "chunk-${index++}.wav")
                output = RandomAccessFile(file!!, "rw").apply { setLength(0); write(ByteArray(44)) }
            }
            val take = minOf(count - offset, chunkSamples - currentSamples, (limitSamples - samples).toInt())
            if (pcmBytes.size < take * 2) pcmBytes = ByteArray(take * 2)
            for (i in 0 until take) {
                val sample = data[offset + i].toInt()
                pcmBytes[i * 2] = sample.toByte()
                pcmBytes[i * 2 + 1] = (sample shr 8).toByte()
            }
            output!!.write(pcmBytes, 0, take * 2)
            samples += take
            currentSamples += take
            offset += take
            if (currentSamples == chunkSamples) flush()
        }
        return samples < limitSamples
    }
    private fun flush() {
        val out = output ?: return
        val ready = file!!
        output = null
        file = null
        val byteCount = currentSamples * 2
        try {
            fun le(value: Int, bytes: Int) { repeat(bytes) { out.write(value ushr (it * 8) and 255) } }
            out.seek(0)
            out.writeBytes("RIFF"); le(36 + byteCount, 4); out.writeBytes("WAVEfmt ")
            le(16, 4); le(1, 2); le(1, 2); le(SAMPLE_RATE, 4); le(SAMPLE_RATE * 2, 4)
            le(2, 2); le(16, 2); out.writeBytes("data"); le(byteCount, 4)
        } finally { out.close() }
        currentSamples = 0
        onChunk(ready)
    }
    fun finish() { if (!ended) { ended = true; flush() } }
    fun cancel() {
        ended = true
        output?.close()
        output = null
        file?.delete()
        file = null
    }
}

package dev.voiceagent.app.speech

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.File

class ContinuousRecorder(
    directory: File,
    onChunk: (File) -> Unit,
    private val onFinished: (String?) -> Unit,
) {
    @Volatile var level = 0f
        private set
    private val chunks = PcmChunks(directory, onChunk = onChunk)
    val seconds: Int get() = (chunks.samples / PcmChunks.SAMPLE_RATE).toInt()
    @Volatile private var running = false
    @Volatile private var cancelled = false
    private var audio: AudioRecord? = null
    private var worker: Thread? = null
    @SuppressLint("MissingPermission") // Activity checks RECORD_AUDIO before starting.
    fun start() {
        val minimum = AudioRecord.getMinBufferSize(PcmChunks.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        check(minimum > 0) { "This device does not support voice recording." }
        val next = AudioRecord.Builder().setAudioSource(MediaRecorder.AudioSource.MIC)
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(PcmChunks.SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_IN_MONO).build())
            .setBufferSizeInBytes(maxOf(minimum * 2, 32000)).build()
        audio = next
        try {
            check(next.state == AudioRecord.STATE_INITIALIZED)
            next.startRecording()
            check(next.recordingState == AudioRecord.RECORDSTATE_RECORDING)
        } catch (e: Exception) { next.release(); audio = null; throw e }
        running = true
        worker = Thread {
            var error: String? = null
            try {
                val buffer = ShortArray(1024)
                while (running) {
                    val count = next.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                    if (count > 0) {
                        var energy = 0.0
                        for (i in 0 until count) { val value = buffer[i].toDouble() / 32768.0; energy += value * value }
                        val rms = kotlin.math.sqrt(energy / count)
                        level = (kotlin.math.ln(1.0 + 24.0 * rms) / kotlin.math.ln(25.0)).toFloat().coerceIn(0f, 1f)
                        if (!chunks.write(buffer, count)) break
                    }
                    else if (running && count < 0) throw java.io.IOException("Microphone recording was interrupted.")
                }
            } catch (e: Exception) { if (!cancelled) error = e.message ?: "Recording failed." }
            finally {
                running = false
                try { next.stop() } catch (_: Exception) {}
                next.release()
                try { if (cancelled) chunks.cancel() else chunks.finish() }
                catch (_: Exception) { error = "Could not save the final audio chunk." }
                onFinished(error)
            }
        }.apply { name = "veya-audio-capture"; start() }
    }
    fun stop(cancel: Boolean = false) {
        if (cancel) cancelled = true
        running = false
        try { audio?.stop() } catch (_: Exception) {}
    }
    fun awaitStop() { worker?.join() }
}

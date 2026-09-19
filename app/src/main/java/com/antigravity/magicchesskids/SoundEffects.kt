package com.antigravity.magicchesskids

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object SoundEffects {
    private val executor = Executors.newSingleThreadExecutor()
    var isMuted = false

    private const val SAMPLE_RATE = 22050

    fun playPop() {
        if (isMuted) return
        executor.execute {
            playToneSweep(550.0, 950.0, 0.07, 0.5)
        }
    }

    fun playMove() {
        if (isMuted) return
        executor.execute {
            playWoodTap(0.06, 0.7)
        }
    }

    fun playCapture() {
        if (isMuted) return
        executor.execute {
            playChord(doubleArrayOf(784.0, 987.77, 1318.51), 0.22, 0.6)
        }
    }

    fun playStarCollect() {
        if (isMuted) return
        executor.execute {
            playArpeggio(doubleArrayOf(523.25, 659.25, 783.99, 1046.50), 0.08, 0.6)
        }
    }

    fun playLevelComplete() {
        if (isMuted) return
        executor.execute {
            playArpeggio(doubleArrayOf(440.0, 554.37, 659.25, 880.0, 1108.73), 0.12, 0.7)
        }
    }

    fun playVictory() {
        if (isMuted) return
        executor.execute {
            playFanfare()
        }
    }

    fun playInvalid() {
        if (isMuted) return
        executor.execute {
            playToneSweep(320.0, 180.0, 0.14, 0.4)
        }
    }

    fun playHint() {
        if (isMuted) return
        executor.execute {
            playToneSweep(880.0, 1174.66, 0.18, 0.5)
        }
    }

    private fun playToneSweep(startFreq: Double, endFreq: Double, durationSec: Double, volume: Double) {
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(numSamples)
        var phase = 0.0

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val freq = startFreq + (endFreq - startFreq) * progress
            val envelope = (1.0 - progress) * (1.0 - progress)
            val sample = sin(phase) * envelope * volume
            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()

            phase += 2.0 * PI * freq / SAMPLE_RATE
            if (phase > 2.0 * PI) phase -= 2.0 * PI
        }

        playBuffer(buffer)
    }

    private fun playWoodTap(durationSec: Double, volume: Double) {
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val decay = exp(-t * 55.0)
            val wave = (sin(2.0 * PI * 180.0 * t) + 0.5 * sin(2.0 * PI * 340.0 * t)) * decay * volume
            buffer[i] = (wave * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        playBuffer(buffer)
    }

    private fun playChord(freqs: DoubleArray, durationSec: Double, volume: Double) {
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val decay = exp(-t * 12.0)
            var sample = 0.0
            for (f in freqs) {
                sample += sin(2.0 * PI * f * t)
            }
            sample = (sample / freqs.size) * decay * volume
            buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        playBuffer(buffer)
    }

    private fun playArpeggio(notes: DoubleArray, noteDurationSec: Double, volume: Double) {
        val noteSamples = (SAMPLE_RATE * noteDurationSec).toInt()
        val totalSamples = noteSamples * notes.size
        val buffer = ShortArray(totalSamples)

        for (n in notes.indices) {
            val freq = notes[n]
            val offset = n * noteSamples
            for (i in 0 until noteSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val decay = exp(-t * 10.0)
                val sample = sin(2.0 * PI * freq * t) * decay * volume
                buffer[offset + i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }

        playBuffer(buffer)
    }

    private fun playFanfare() {
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
        val noteSamples = (SAMPLE_RATE * 0.12).toInt()
        val finalSamples = (SAMPLE_RATE * 0.45).toInt()
        val totalSamples = noteSamples * 3 + finalSamples
        val buffer = ShortArray(totalSamples)

        var offset = 0
        for (n in 0..2) {
            val freq = notes[n]
            for (i in 0 until noteSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val decay = exp(-t * 8.0)
                val sample = sin(2.0 * PI * freq * t) * decay * 0.6
                buffer[offset + i] = (sample * Short.MAX_VALUE).toInt().toShort()
            }
            offset += noteSamples
        }

        val lastFreq = notes[3]
        for (i in 0 until finalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val decay = exp(-t * 4.0)
            val sample = (sin(2.0 * PI * lastFreq * t) + 0.3 * sin(2.0 * PI * lastFreq * 1.5 * t)) * decay * 0.65
            buffer[offset + i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }

        playBuffer(buffer)
    }

    private fun playBuffer(buffer: ShortArray) {
        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            Thread.sleep((buffer.size.toDouble() / SAMPLE_RATE * 1000).toLong() + 30)
            audioTrack.stop()
            audioTrack.release()
        } catch (_: Exception) {}
    }
}

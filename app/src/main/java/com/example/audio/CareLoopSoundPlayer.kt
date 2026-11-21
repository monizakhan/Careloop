package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

/**
 * Plays a short, soft chime when a new notification arrives after user interaction.
 * Safely wrapped in try/catch so unit tests and headless environments never fail.
 */
object CareLoopSoundPlayer {
  @Volatile
  var hasUserInteracted: Boolean = false

  @Volatile
  var isSoundEnabled: Boolean = true

  fun markUserInteracted() {
    hasUserInteracted = true
  }

  fun playSoftNotificationBeep() {
    if (!hasUserInteracted || !isSoundEnabled) return
    try {
      Thread {
        try {
          val sampleRate = 22050
          val durationMs = 150
          val numSamples = sampleRate * durationMs / 1000
          val samples = ShortArray(numSamples)
          for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val freq = if (i < numSamples / 2) 784.0 else 987.77
            val env = if (i < numSamples * 0.15) {
              i / (numSamples * 0.15)
            } else {
              1.0 - ((i - numSamples * 0.15) / (numSamples * 0.85))
            }
            val valDouble = sin(2.0 * PI * freq * t) * env * 0.18
            samples[i] = (valDouble * Short.MAX_VALUE).toInt().coerceIn(
              Short.MIN_VALUE.toInt(),
              Short.MAX_VALUE.toInt(),
            ).toShort()
          }
          val bufferBytes = samples.size * 2
          val track = AudioTrack.Builder()
            .setAudioAttributes(
              AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            )
            .setAudioFormat(
              AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            )
            .setBufferSizeInBytes(bufferBytes)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
          track.write(samples, 0, samples.size)
          track.play()
          Thread.sleep(durationMs.toLong() + 40L)
          track.stop()
          track.release()
        } catch (_: Throwable) {
        }
      }.start()
    } catch (_: Throwable) {
    }
  }
}

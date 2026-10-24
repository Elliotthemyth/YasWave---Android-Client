package com.example.data

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

object AudioSynthesizer {

    /**
     * Synthesizes a pleasant melodic synthwave loop into a standard PCM 16-bit 44.1kHz WAV file.
     */
    fun createSynthesizedTrack(outputFile: File, durationSeconds: Int = 25, chordBaseFreq: Double = 220.0) {
        if (outputFile.exists() && outputFile.length() > 44) return

        val sampleRate = 44100
        val totalSamples = sampleRate * durationSeconds
        val dataSize = totalSamples * 2 // 16-bit mono = 2 bytes per sample

        FileOutputStream(outputFile).use { fos ->
            writeWavHeader(fos, sampleRate, 1, 16, dataSize)

            val buffer = ByteArray(1024)
            var bufferIdx = 0

            // Melodic arpeggio frequencies (A minor / Pentatonic vibes)
            val notes = doubleArrayOf(
                chordBaseFreq,
                chordBaseFreq * 1.189, // Minor third
                chordBaseFreq * 1.335, // Fourth
                chordBaseFreq * 1.498, // Fifth
                chordBaseFreq * 1.782, // Minor 7th
                chordBaseFreq * 2.0    // Octave
            )

            val beatSamples = (sampleRate * 0.25).toInt() // 120 BPM 16th notes
            var phaseArp = 0.0
            var phaseBass = 0.0
            var phasePad = 0.0

            for (i in 0 until totalSamples) {
                val step = (i / beatSamples) % notes.size
                val currentFreq = notes[step]
                val bassFreq = chordBaseFreq * 0.5

                // Smooth envelope per arpeggio note
                val noteProgress = (i % beatSamples).toDouble() / beatSamples
                val envelope = Math.exp(-3.0 * noteProgress)

                // Sine waves synthesis
                phaseArp += 2.0 * Math.PI * currentFreq / sampleRate
                phaseBass += 2.0 * Math.PI * bassFreq / sampleRate
                phasePad += 2.0 * Math.PI * (chordBaseFreq * 1.498) / sampleRate

                val arpSample = sin(phaseArp) * envelope * 0.4
                val bassSample = sin(phaseBass) * 0.35
                val padSample = sin(phasePad) * 0.15

                val mixed = (arpSample + bassSample + padSample).coerceIn(-1.0, 1.0)
                val sample16 = (mixed * 32767.0).toInt().toShort()

                // Little-endian short
                buffer[bufferIdx++] = (sample16.toInt() and 0xFF).toByte()
                buffer[bufferIdx++] = ((sample16.toInt() shr 8) and 0xFF).toByte()

                if (bufferIdx >= buffer.size) {
                    fos.write(buffer, 0, bufferIdx)
                    bufferIdx = 0
                }
            }

            if (bufferIdx > 0) {
                fos.write(buffer, 0, bufferIdx)
            }
        }
    }

    private fun writeWavHeader(fos: FileOutputStream, sampleRate: Int, channels: Int, bitsPerSample: Int, dataSize: Int) {
        val totalFileSize = 36 + dataSize
        val byteRate = sampleRate * channels * (bitsPerSample / 8)
        val blockAlign = channels * (bitsPerSample / 8)

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt(totalFileSize)
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16) // Subchunk1Size for PCM
        header.putShort(1.toShort()) // AudioFormat 1 = PCM
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort(blockAlign.toShort())
        header.putShort(bitsPerSample.toShort())
        header.put("data".toByteArray())
        header.putInt(dataSize)

        fos.write(header.array())
    }
}

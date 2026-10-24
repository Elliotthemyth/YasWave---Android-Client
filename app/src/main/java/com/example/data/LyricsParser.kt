package com.example.data

import com.example.model.LyricLine
import java.util.regex.Pattern

object LyricsParser {
    private val LRC_TIMESTAMP_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})(?:\\.(\\d{2,3}))?]")

    /**
     * Checks if content contains standard LRC timestamp tags like [01:23.45]
     */
    fun hasLrcTimestamps(content: String?): Boolean {
        if (content.isNullOrBlank()) return false
        return LRC_TIMESTAMP_PATTERN.matcher(content).find()
    }

    /**
     * Determines whether content is synchronized LRC or plain text lyrics
     */
    fun determineLyricsMode(content: String?): String {
        return if (hasLrcTimestamps(content)) "SYNCED" else "PLAIN"
    }

    /**
     * Parses synchronized lyrics from LRC format.
     * Supports both embedded Persian translations (lines starting with [fa] or [tr])
     * or parallel Persian text blocks.
     */
    fun parseLrc(lrcContent: String?, persianTranslationContent: String? = null): List<LyricLine> {
        if (lrcContent.isNullOrBlank()) return emptyList()

        val lines = lrcContent.lines()
        val parsedLines = mutableListOf<LyricLine>()

        // Check if persian translations are supplied separately
        val persianLines = persianTranslationContent?.lines()?.mapNotNull { line ->
            parseLine(line)
        }?.sortedBy { it.timeMs }

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("[ti:") || line.startsWith("[ar:") || line.startsWith("[al:")) {
                continue
            }

            val matcher = LRC_TIMESTAMP_PATTERN.matcher(line)
            val timestamps = mutableListOf<Long>()
            var lastMatchEnd = 0

            while (matcher.find()) {
                val min = matcher.group(1)?.toLongOrNull() ?: 0L
                val sec = matcher.group(2)?.toLongOrNull() ?: 0L
                val millisRaw = matcher.group(3)
                val millis = when {
                    millisRaw == null -> 0L
                    millisRaw.length == 2 -> millisRaw.toLong() * 10
                    millisRaw.length == 3 -> millisRaw.toLong()
                    else -> millisRaw.take(3).toLong()
                }
                val totalMs = (min * 60 * 1000) + (sec * 1000) + millis
                timestamps.add(totalMs)
                lastMatchEnd = matcher.end()
            }

            if (timestamps.isNotEmpty()) {
                val lyricText = line.substring(lastMatchEnd).trim()
                for (time in timestamps) {
                    // Check if line contains inline Persian translation e.g. "English | ترجمه فارسی"
                    var englishText = lyricText
                    var inlinePersian: String? = null
                    if (lyricText.contains(" | ")) {
                        val split = lyricText.split(" | ", limit = 2)
                        englishText = split[0].trim()
                        inlinePersian = split[1].trim()
                    } else if (lyricText.contains(" // ")) {
                        val split = lyricText.split(" // ", limit = 2)
                        englishText = split[0].trim()
                        inlinePersian = split[1].trim()
                    }

                    parsedLines.add(LyricLine(timeMs = time, text = englishText, persianText = inlinePersian))
                }
            }
        }

        parsedLines.sortBy { it.timeMs }

        // If a separate Persian translation file/block was provided, map by closest timestamp
        if (!persianLines.isNullOrEmpty() && parsedLines.isNotEmpty()) {
            return parsedLines.map { orig ->
                if (orig.persianText != null) {
                    orig
                } else {
                    val matchingFa = persianLines.minByOrNull { Math.abs(it.timeMs - orig.timeMs) }
                    if (matchingFa != null && Math.abs(matchingFa.timeMs - orig.timeMs) <= 1500) {
                        orig.copy(persianText = matchingFa.text)
                    } else {
                        orig
                    }
                }
            }
        }

        return parsedLines
    }

    private fun parseLine(line: String): LyricLine? {
        val matcher = LRC_TIMESTAMP_PATTERN.matcher(line.trim())
        if (matcher.find()) {
            val min = matcher.group(1)?.toLongOrNull() ?: 0L
            val sec = matcher.group(2)?.toLongOrNull() ?: 0L
            val millisRaw = matcher.group(3)
            val millis = when {
                millisRaw == null -> 0L
                millisRaw.length == 2 -> millisRaw.toLong() * 10
                else -> millisRaw.take(3).toLong()
            }
            val totalMs = (min * 60 * 1000) + (sec * 1000) + millis
            val text = line.substring(matcher.end()).trim()
            return LyricLine(timeMs = totalMs, text = text)
        }
        return null
    }

    /**
     * Converts a list of LyricLines back to standard LRC string format
     */
    fun formatToLrc(lines: List<LyricLine>): String {
        val sb = StringBuilder()
        for (item in lines) {
            val totalSec = item.timeMs / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            val ms = (item.timeMs % 1000) / 10
            val lineText = if (item.persianText != null) "${item.text} | ${item.persianText}" else item.text
            sb.append(String.format("[%02d:%02d.%02d] %s\n", min, sec, ms, lineText))
        }
        return sb.toString()
    }

    /**
     * Finds active lyric line index for current playback position
     */
    fun findActiveLyricIndex(lyrics: List<LyricLine>, currentPositionMs: Long): Int {
        if (lyrics.isEmpty()) return -1
        for (i in lyrics.indices.reversed()) {
            if (currentPositionMs >= lyrics[i].timeMs) {
                return i
            }
        }
        return 0
    }
}

package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

sealed class LrclibException(message: String, val statusCode: Int? = null) : Exception(message) {
    class AccessForbiddenException(
        message: String = "Access forbidden (HTTP 403). LRCLIB rejected client access or request headers."
    ) : LrclibException(message, 403)

    class RateLimitedException(
        message: String = "Too many requests (HTTP 429). Rate limit reached; please wait before searching again."
    ) : LrclibException(message, 429)

    class ServerException(
        val code: Int,
        message: String = "LRCLIB service error (HTTP $code)."
    ) : LrclibException(message, code)

    class NetworkException(
        message: String,
        cause: Throwable? = null
    ) : LrclibException(message)

    class ParseException(
        message: String,
        cause: Throwable? = null
    ) : LrclibException(message)
}

data class LrclibCandidate(
    val id: Long,
    val trackName: String,
    val artistName: String,
    val albumName: String?,
    val durationSeconds: Int,
    val syncedLyrics: String?,
    val plainLyrics: String?,
    val instrumental: Boolean
) {
    val hasSyncedLyrics: Boolean
        get() = !syncedLyrics.isNullOrBlank()

    val hasPlainLyrics: Boolean
        get() = !plainLyrics.isNullOrBlank()

    val formattedDuration: String
        get() {
            val min = durationSeconds / 60
            val sec = durationSeconds % 60
            return "%d:%02d".format(min, sec)
        }
}

object LrclibService {
    private const val TAG = "LrclibService"
    private const val SEARCH_URL = "https://lrclib.net/api/search"
    private const val GET_URL = "https://lrclib.net/api/get"

    // Documented format: AppName app-version (contact-url-or-email)
    const val USER_AGENT = "YASWAVE 2.0 (https://github.com/Elliotthemyth/YasWave)"

    var okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun searchLyrics(trackName: String, artistName: String? = null): Result<List<LrclibCandidate>> = withContext(Dispatchers.IO) {
        val trimmedTrack = trackName.trim()
        val trimmedArtist = artistName?.trim()

        if (trimmedTrack.isBlank()) {
            return@withContext Result.success(emptyList())
        }

        try {
            val queryParam = if (!trimmedArtist.isNullOrBlank() && trimmedArtist != "Unknown Artist") {
                "track_name=${URLEncoder.encode(trimmedTrack, "UTF-8")}&artist_name=${URLEncoder.encode(trimmedArtist, "UTF-8")}"
            } else {
                "q=${URLEncoder.encode(trimmedTrack, "UTF-8")}"
            }

            val request = Request.Builder()
                .url("$SEARCH_URL?$queryParam")
                .header("User-Agent", USER_AGENT)
                .header("Lrclib-Client", USER_AGENT)
                .header("X-User-Agent", USER_AGENT)
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val code = response.code
                Log.d(TAG, "LRCLIB query status: HTTP $code")

                when (code) {
                    200 -> {
                        val body = response.body?.string() ?: return@withContext Result.success(emptyList())
                        val candidates = parseJsonArrayCandidates(body)
                        Result.success(candidates)
                    }
                    403 -> {
                        Log.w(TAG, "LRCLIB returned 403 Forbidden. Client header or access restriction.")
                        Result.failure(LrclibException.AccessForbiddenException())
                    }
                    429 -> {
                        Log.w(TAG, "LRCLIB returned 429 Too Many Requests.")
                        Result.failure(LrclibException.RateLimitedException())
                    }
                    404 -> {
                        Result.success(emptyList())
                    }
                    in 500..599 -> {
                        Log.w(TAG, "LRCLIB server error HTTP $code")
                        Result.failure(LrclibException.ServerException(code, "LRCLIB service temporarily unavailable (HTTP $code)"))
                    }
                    else -> {
                        Result.failure(LrclibException.ServerException(code, "LRCLIB returned HTTP $code"))
                    }
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "Network exception querying LRCLIB: ${e.message}")
            Result.failure(LrclibException.NetworkException("Unable to connect to LRCLIB: ${e.message}", e))
        } catch (e: Exception) {
            Log.w(TAG, "Unexpected exception querying LRCLIB: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Parses a JSON array response from LRCLIB into list of candidates
     */
    fun parseJsonArrayCandidates(body: String): List<LrclibCandidate> {
        val candidates = mutableListOf<LrclibCandidate>()
        if (body.isBlank()) return candidates

        val jsonArray = JSONArray(body)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            candidates.add(parseCandidateObject(obj))
        }
        return candidates
    }

    /**
     * Parses a single candidate JSON object from LRCLIB
     */
    fun parseCandidateObject(obj: JSONObject): LrclibCandidate {
        return LrclibCandidate(
            id = obj.optLong("id", 0L),
            trackName = obj.optString("trackName", obj.optString("name", "Unknown")),
            artistName = obj.optString("artistName", "Unknown Artist"),
            albumName = obj.optString("albumName").takeIf { it.isNotBlank() },
            durationSeconds = obj.optInt("duration", 0),
            syncedLyrics = obj.optString("syncedLyrics").takeIf { it.isNotBlank() },
            plainLyrics = obj.optString("plainLyrics").takeIf { it.isNotBlank() },
            instrumental = obj.optBoolean("instrumental", false)
        )
    }
}

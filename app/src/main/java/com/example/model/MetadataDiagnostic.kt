package com.example.model

data class MetadataDiagnostic(
    val trackId: String,
    val title: String,
    val rawArtistKey: String?,
    val rawAlbumArtistKey: String?,
    val rawAuthorKey: String?,
    val rawComposerKey: String?,
    val rawTrackNumberKey: String?,
    val rawDiscNumberKey: String?,
    val rawGenreKey: String?,
    val resolvedArtist: String,
    val diagnosisNote: String,
    val detectionSource: String
) {
    fun toJsonString(): String {
        return """
        {
          "trackId": "$trackId",
          "title": "${escapeJson(title)}",
          "rawArtistKey": ${rawArtistKey?.let { "\"${escapeJson(it)}\"" } ?: "null"},
          "rawAlbumArtistKey": ${rawAlbumArtistKey?.let { "\"${escapeJson(it)}\"" } ?: "null"},
          "rawAuthorKey": ${rawAuthorKey?.let { "\"${escapeJson(it)}\"" } ?: "null"},
          "rawComposerKey": ${rawComposerKey?.let { "\"${escapeJson(it)}\"" } ?: "null"},
          "rawTrackNumberKey": ${rawTrackNumberKey?.let { "\"${escapeJson(it)}\"" } ?: "null"},
          "rawDiscNumberKey": ${rawDiscNumberKey?.let { "\"${escapeJson(it)}\"" } ?: "null"},
          "rawGenreKey": ${rawGenreKey?.let { "\"${escapeJson(it)}\"" } ?: "null"},
          "resolvedArtist": "${escapeJson(resolvedArtist)}",
          "detectionSource": "${escapeJson(detectionSource)}",
          "diagnosisNote": "${escapeJson(diagnosisNote)}"
        }
        """.trimIndent()
    }

    private fun escapeJson(str: String): String =
        str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")
}

package com.example.model

data class Playlist(
    val id: String,
    val name: String,
    val description: String = "",
    val trackIds: List<String> = emptyList(),
    val coverResId: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)

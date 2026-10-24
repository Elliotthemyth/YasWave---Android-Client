package com.example.playback

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.Visualizer
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sin

enum class RepeatMode {
    OFF, ALL, ONE
}

enum class PlayerState {
    IDLE,
    INITIALIZED,
    PREPARING,
    PREPARED,
    STARTED,
    PAUSED,
    COMPLETED,
    ERROR
}

data class EqualizerPreset(
    val name: String,
    val bands: List<Float>, // 5 bands: 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz
    val bassBoost: Float = 0.5f
)

class AudioPlayerEngine(private val context: Context) {
    private val TAG = "AudioPlayerEngine"
    private var mediaPlayer: MediaPlayer? = null
    private var playerState = PlayerState.IDLE
    private var visualizer: Visualizer? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null
    private var fallbackVisualizerJob: Job? = null

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var resumeOnFocusGain = false
    private var isBecomingNoisyRegistered = false

    // Playback state
    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.ALL)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    // 24-band frequency spectrum for the live wave visualizer (0.0f .. 1.0f)
    private val _spectrumBands = MutableStateFlow(List(24) { 0.05f })
    val spectrumBands: StateFlow<List<Float>> = _spectrumBands.asStateFlow()

    private val _isHardwareVisualizerActive = MutableStateFlow(false)
    val isHardwareVisualizerActive: StateFlow<Boolean> = _isHardwareVisualizerActive.asStateFlow()

    // Equalizer
    private val presets = listOf(
        EqualizerPreset("Flat", listOf(0f, 0f, 0f, 0f, 0f), 0.2f),
        EqualizerPreset("Bass Boost", listOf(0.8f, 0.6f, 0.1f, -0.1f, -0.2f), 0.9f),
        EqualizerPreset("Synthwave", listOf(0.7f, 0.4f, 0.0f, 0.5f, 0.8f), 0.75f),
        EqualizerPreset("Persian Traditional", listOf(0.2f, 0.5f, 0.7f, 0.6f, 0.3f), 0.4f),
        EqualizerPreset("Acoustic", listOf(0.4f, 0.3f, 0.2f, 0.5f, 0.4f), 0.3f),
        EqualizerPreset("Vocal Boost", listOf(-0.2f, 0.2f, 0.8f, 0.7f, 0.2f), 0.25f),
        EqualizerPreset("Electronic", listOf(0.8f, 0.5f, -0.2f, 0.6f, 0.7f), 0.85f),
        EqualizerPreset("Rock", listOf(0.6f, 0.4f, -0.1f, 0.5f, 0.7f), 0.6f)
    )

    private val _activePreset = MutableStateFlow(presets[0]) // Default Flat / Clean
    val activePreset: StateFlow<EqualizerPreset> = _activePreset.asStateFlow()

    val availablePresets: List<EqualizerPreset> = presets

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                // Pause playback when headphones or external audio is unplugged
                if (_isPlaying.value) {
                    togglePlayPause()
                }
            }
        }
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                resumeOnFocusGain = false
                if (_isPlaying.value) {
                    if (playerState == PlayerState.STARTED) {
                        try { mediaPlayer?.pause() } catch (_: Exception) {}
                        playerState = PlayerState.PAUSED
                    }
                    _isPlaying.value = false
                    stopProgressTracker()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                resumeOnFocusGain = _isPlaying.value
                if (_isPlaying.value) {
                    if (playerState == PlayerState.STARTED) {
                        try { mediaPlayer?.pause() } catch (_: Exception) {}
                        playerState = PlayerState.PAUSED
                    }
                    _isPlaying.value = false
                    stopProgressTracker()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (playerState == PlayerState.STARTED || playerState == PlayerState.PAUSED) {
                    try { mediaPlayer?.setVolume(0.2f, 0.2f) } catch (_: Exception) {}
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (playerState == PlayerState.STARTED || playerState == PlayerState.PAUSED) {
                    try { mediaPlayer?.setVolume(1.0f, 1.0f) } catch (_: Exception) {}
                }
                if (resumeOnFocusGain) {
                    resumeOnFocusGain = false
                    if (playerState == PlayerState.PAUSED || playerState == PlayerState.PREPARED) {
                        try {
                            mediaPlayer?.start()
                            playerState = PlayerState.STARTED
                            _isPlaying.value = true
                            startProgressTracker()
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    init {
        registerBecomingNoisy()
        startFallbackVisualizerEngine()
    }

    fun playTrack(track: Track, newQueue: List<Track>? = null) {
        if (newQueue != null) {
            _queue.value = newQueue
        } else if (!_queue.value.any { it.id == track.id }) {
            _queue.value = listOf(track) + _queue.value
        }

        _currentTrack.value = track
        setupAndPlay(track)
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest = req
            audioManager.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    private fun registerBecomingNoisy() {
        if (!isBecomingNoisyRegistered) {
            val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            context.registerReceiver(becomingNoisyReceiver, filter)
            isBecomingNoisyRegistered = true
        }
    }

    private fun unregisterBecomingNoisy() {
        if (isBecomingNoisyRegistered) {
            try {
                context.unregisterReceiver(becomingNoisyReceiver)
            } catch (_: Exception) {}
            isBecomingNoisyRegistered = false
        }
    }

    private fun setupAndPlay(track: Track) {
        releasePlayer()
        requestAudioFocus()
        playerState = PlayerState.IDLE

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                if (track.path.isNotBlank() && File(track.path).exists()) {
                    setDataSource(track.path)
                } else if (track.uriString.isNotBlank()) {
                    val uri = Uri.parse(track.uriString)
                    if (uri.scheme == "file") {
                        val path = uri.path ?: ""
                        if (File(path).exists()) {
                            setDataSource(path)
                        } else {
                            setDataSource(context, uri)
                        }
                    } else if (uri.scheme == "content") {
                        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                            setDataSource(pfd.fileDescriptor)
                        } ?: setDataSource(context, uri)
                    } else {
                        setDataSource(context, uri)
                    }
                } else {
                    Log.w(TAG, "No valid source for track ${track.title}")
                    return
                }

                playerState = PlayerState.INITIALIZED

                setOnPreparedListener { mp ->
                    if (mediaPlayer !== mp) return@setOnPreparedListener
                    playerState = PlayerState.PREPARED
                    try {
                        mp.start()
                        playerState = PlayerState.STARTED
                        _isPlaying.value = true
                        _durationMs.value = mp.duration.toLong()
                        startProgressTracker()
                        attachHardwareVisualizer(mp.audioSessionId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start playback after prepare: ${e.message}")
                        playerState = PlayerState.ERROR
                        _isPlaying.value = false
                    }
                }

                setOnCompletionListener { mp ->
                    if (mediaPlayer !== mp) return@setOnCompletionListener
                    playerState = PlayerState.COMPLETED
                    handleTrackCompletion()
                }

                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    if (mediaPlayer === mp) {
                        playerState = PlayerState.ERROR
                        _isPlaying.value = false
                        stopProgressTracker()
                    }
                    true
                }

                playerState = PlayerState.PREPARING
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize playback: ${e.message}", e)
            playerState = PlayerState.ERROR
            _isPlaying.value = false
        }
    }

    private fun attachHardwareVisualizer(audioSessionId: Int) {
        releaseVisualizer()
        if (audioSessionId == 0) return

        val hasRecordPerm = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasRecordPerm) {
            _isHardwareVisualizerActive.value = false
            return
        }

        try {
            val vis = Visualizer(audioSessionId).apply {
                val range = Visualizer.getCaptureSizeRange()
                captureSize = range[0].coerceAtLeast(64)
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) {}

                        override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                            if (fft != null && _isPlaying.value) {
                                processFft(fft)
                            }
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    false,
                    true
                )
                enabled = true
            }
            visualizer = vis
            _isHardwareVisualizerActive.value = true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize hardware Visualizer: ${e.message}")
            _isHardwareVisualizerActive.value = false
        }
    }

    private fun processFft(fft: ByteArray) {
        // Compute 24 magnitude bins from FFT data
        val n = fft.size / 2
        val bands = MutableList(24) { 0.05f }
        val step = (n / 24).coerceAtLeast(1)

        for (i in 0 until 24) {
            val idx = (i * step).coerceIn(0, n - 1)
            val r = fft[2 * idx].toFloat()
            val im = fft[2 * idx + 1].toFloat()
            val magnitude = hypot(r, im) / 64f
            val smoothed = (0.7f * _spectrumBands.value[i] + 0.3f * magnitude).coerceIn(0.05f, 0.95f)
            bands[i] = smoothed
        }
        _spectrumBands.value = bands
    }

    private fun releaseVisualizer() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (_: Exception) {}
        visualizer = null
        _isHardwareVisualizerActive.value = false
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: run {
            _currentTrack.value?.let { playTrack(it) }
            return
        }

        if (playerState == PlayerState.PREPARING) {
            // Player is preparing; wait for prepared callback
            return
        }

        if (playerState == PlayerState.STARTED) {
            try {
                player.pause()
                playerState = PlayerState.PAUSED
                _isPlaying.value = false
                stopProgressTracker()
            } catch (e: Exception) {
                Log.w(TAG, "Error pausing playback: ${e.message}")
            }
        } else if (playerState == PlayerState.PAUSED || playerState == PlayerState.PREPARED || playerState == PlayerState.COMPLETED) {
            try {
                requestAudioFocus()
                player.start()
                playerState = PlayerState.STARTED
                _isPlaying.value = true
                startProgressTracker()
            } catch (e: Exception) {
                Log.w(TAG, "Error resuming playback: ${e.message}")
            }
        }
    }

    fun seekTo(positionMs: Long) {
        _currentPositionMs.value = positionMs
        if (playerState == PlayerState.PREPARED || playerState == PlayerState.STARTED || playerState == PlayerState.PAUSED || playerState == PlayerState.COMPLETED) {
            try {
                mediaPlayer?.seekTo(positionMs.toInt())
            } catch (e: Exception) {
                Log.w(TAG, "Error seeking: ${e.message}")
            }
        }
    }

    fun skipNext() {
        val q = _queue.value
        if (q.isEmpty()) return

        val current = _currentTrack.value
        val currentIndex = q.indexOfFirst { it.id == current?.id }

        val nextIndex = if (_isShuffle.value) {
            (q.indices - currentIndex).randomOrNull() ?: 0
        } else {
            if (currentIndex + 1 < q.size) currentIndex + 1 else 0
        }

        playTrack(q[nextIndex])
    }

    fun skipPrevious() {
        if (playerState == PlayerState.STARTED || playerState == PlayerState.PAUSED) {
            val currentPos = try { mediaPlayer?.currentPosition ?: 0 } catch (_: Exception) { 0 }
            if (currentPos > 3000) {
                seekTo(0)
                return
            }
        }

        val q = _queue.value
        if (q.isEmpty()) return

        val current = _currentTrack.value
        val currentIndex = q.indexOfFirst { it.id == current?.id }

        val prevIndex = if (currentIndex > 0) currentIndex - 1 else q.size - 1
        playTrack(q[prevIndex])
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun cycleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun selectPreset(preset: EqualizerPreset) {
        _activePreset.value = preset
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        val list = _queue.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _queue.value = list
        }
    }

    fun removeFromQueue(trackId: String) {
        _queue.value = _queue.value.filter { it.id != trackId }
    }

    private fun handleTrackCompletion() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                seekTo(0)
                if (playerState == PlayerState.COMPLETED || playerState == PlayerState.PAUSED || playerState == PlayerState.PREPARED) {
                    try {
                        mediaPlayer?.start()
                        playerState = PlayerState.STARTED
                        _isPlaying.value = true
                        startProgressTracker()
                    } catch (_: Exception) {
                        _currentTrack.value?.let { playTrack(it) }
                    }
                } else {
                    _currentTrack.value?.let { playTrack(it) }
                }
            }
            RepeatMode.ALL -> skipNext()
            RepeatMode.OFF -> {
                val q = _queue.value
                val current = _currentTrack.value
                val currentIndex = q.indexOfFirst { it.id == current?.id }
                if (currentIndex != -1 && currentIndex < q.size - 1) {
                    skipNext()
                } else {
                    _isPlaying.value = false
                    seekTo(0)
                }
            }
        }
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = scope.launch {
            while (isActive) {
                if (playerState == PlayerState.STARTED) {
                    mediaPlayer?.let { player ->
                        try {
                            if (player.isPlaying) {
                                _currentPositionMs.value = player.currentPosition.toLong()
                            }
                        } catch (_: Exception) {}
                    }
                }
                delay(150)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun startFallbackVisualizerEngine() {
        fallbackVisualizerJob?.cancel()
        fallbackVisualizerJob = scope.launch {
            var tick = 0.0
            while (isActive) {
                tick += 0.15
                // Only run fallback simulation if hardware visualizer is not active
                if (!_isHardwareVisualizerActive.value) {
                    if (_isPlaying.value) {
                        val presetBoost = _activePreset.value.bassBoost
                        val bands = List(24) { index ->
                            val baseWave = sin(tick * 1.8 + index * 0.4)
                            val beatWave = abs(sin(tick * 3.2))
                            val freqFactor = if (index < 6) presetBoost * 1.2 else 0.8
                            val value = ((abs(baseWave) * 0.6 + beatWave * 0.4) * freqFactor)
                                .coerceIn(0.08, 0.98)
                            value.toFloat()
                        }
                        _spectrumBands.value = bands
                    } else {
                        val idle = List(24) { index ->
                            (abs(sin(tick * 0.5 + index * 0.2)) * 0.12 + 0.04).toFloat()
                        }
                        _spectrumBands.value = idle
                    }
                }
                delay(40)
            }
        }
    }

    private fun releasePlayer() {
        stopProgressTracker()
        releaseVisualizer()
        abandonAudioFocus()
        playerState = PlayerState.IDLE
        val p = mediaPlayer
        mediaPlayer = null
        if (p != null) {
            try {
                p.reset()
                p.release()
            } catch (_: Exception) {}
        }
    }

    fun onDestroy() {
        unregisterBecomingNoisy()
        releasePlayer()
        fallbackVisualizerJob?.cancel()
    }
}

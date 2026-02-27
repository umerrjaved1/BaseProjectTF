package com.professor.baseproject.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.professor.baseproject.cache.CacheManager
import com.professor.baseproject.cache.CacheType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AudioPlayerManager : DefaultLifecycleObserver {

    private var mediaPlayer: MediaPlayer? = null
    private var currentUrl: String? = null
    private var isPrepared = false
    private var onStateChanged: ((Boolean) -> Unit)? = null
    var shouldPlay = true

    fun play(
        context: Context,
        url: String,
        onPrepared: () -> Unit = {},
        onCompletion: () -> Unit = {},
        onError: ((String) -> Unit)? = null,
        onStateChange: ((Boolean) -> Unit)? = null,
        lifecycle: Lifecycle? = null
    ) {

        Log.e("TAG", "play: ")
        lifecycle?.addObserver(this) // 🔁 Watch lifecycle

        CoroutineScope(Dispatchers.Main).launch {
            val file = CacheManager.downloadAndCacheIfNeeded(context, url, CacheType.AUDIO)
            if (file == null || !file.exists()) {
                onError?.invoke("Audio file not found.")
                return@launch
            }

            val path = file.absolutePath
            shouldPlay = true
            if (mediaPlayer != null && currentUrl != path) {
                stop()
            }

            if (mediaPlayer != null && isPrepared && currentUrl == path) {
                Log.e("TAG", "play.....: ")
                mediaPlayer?.start()
                onStateChange?.invoke(true)
                onStateChanged = onStateChange
                return@launch
            }

            currentUrl = path
            isPrepared = false
            onStateChanged = onStateChange

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(path)

                setOnPreparedListener {

                    if (shouldPlay) {
                        Log.e("TAG", "play..to...: ")
                        start()
                        onPrepared()
                        onStateChanged?.invoke(true)
                    }
                }

                setOnCompletionListener {
                    onCompletion()
                    onStateChanged?.invoke(false)
                }

                setOnErrorListener { _, what, extra ->
                    onError?.invoke("Error: $what / $extra")
                    stop()
                    true
                }

                prepareAsync()
            }
        }
    }

    fun pause() {
        mediaPlayer?.pause()
        onStateChanged?.invoke(false)
    }

    fun stop() {
        Log.d("Application", "Audio manager stop")
        mediaPlayer?.release()
        mediaPlayer = null
        isPrepared = false
        onStateChanged?.invoke(false)
    }

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true

    fun isPlayingUrl(url: String): Boolean {
        return currentUrl?.endsWith(CacheManager.getSafeFileName(url)) == true && isPlaying()
    }

    // 🔁 Stop when lifecycle is destroyed
    override fun onDestroy(owner: LifecycleOwner) {
        stop()
    }

    // Optional: pause onStop if needed
    override fun onStop(owner: LifecycleOwner) {
        pause()
    }
}

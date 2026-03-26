package com.focusfirst.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import com.focusfirst.data.model.AmbientSound

class SoundManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var mediaPlayer: MediaPlayer? = null
    private var currentSound: AmbientSound = AmbientSound.NONE
    private var currentVolume: Float = 0.5f

    // AudioFocus bookkeeping
    private var audioFocusRequest: AudioFocusRequest? = null  // API 26+
    private var hasFocus = false

    private val focusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasFocus = true
                mediaPlayer?.setVolume(currentVolume, currentVolume)
                if (mediaPlayer?.isPlaying == false) {
                    mediaPlayer?.start()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                hasFocus = false
                mediaPlayer?.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Lower to 30 % while another app ducks us
                mediaPlayer?.setVolume(currentVolume * 0.3f, currentVolume * 0.3f)
            }
        }
    }

    fun play(sound: AmbientSound, volume: Float = currentVolume) {
        if (sound == AmbientSound.NONE) {
            stop()
            return
        }
        if (currentSound == sound && mediaPlayer?.isPlaying == true) {
            setVolume(volume)
            return
        }
        stop()
        currentSound  = sound
        currentVolume = volume

        if (!requestAudioFocus()) {
            Log.w(TAG, "Audio focus denied — not starting playback")
            return
        }

        try {
            val afd = context.assets.openFd(sound.fileName)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                isLooping = true
                setVolume(volume, volume)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play ${sound.fileName}", e)
            abandonAudioFocus()
        }
    }

    fun pause() {
        mediaPlayer?.pause()
    }

    fun resume() {
        if (hasFocus) {
            mediaPlayer?.start()
        }
    }

    fun stop() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer  = null
        currentSound = AmbientSound.NONE
        abandonAudioFocus()
    }

    fun setVolume(volume: Float) {
        currentVolume = volume
        mediaPlayer?.setVolume(volume, volume)
    }

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true

    fun getCurrentSound(): AmbientSound = currentSound

    fun release() {
        stop()
    }

    // ── AudioFocus helpers ────────────────────────────────────────────────────

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener(focusListener)
                .build()
            audioFocusRequest = req
            val result = audioManager.requestAudioFocus(req)
            hasFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            hasFocus
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager.requestAudioFocus(
                focusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
            )
            hasFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            hasFocus
        }
    }

    private fun abandonAudioFocus() {
        if (!hasFocus) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusListener)
        }
        hasFocus = false
    }

    companion object {
        private const val TAG = "SoundManager"
    }
}

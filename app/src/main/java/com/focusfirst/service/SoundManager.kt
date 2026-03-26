package com.focusfirst.service

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.focusfirst.data.model.AmbientSound

class SoundManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var currentSound: AmbientSound = AmbientSound.NONE
    private var currentVolume: Float = 0.5f

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
        }
    }

    fun pause() {
        mediaPlayer?.pause()
    }

    fun resume() {
        mediaPlayer?.start()
    }

    fun stop() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer    = null
        currentSound   = AmbientSound.NONE
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

    companion object {
        private const val TAG = "SoundManager"
    }
}

package com.rtnp.demo.audio

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.IOException
import java.util.*

object AudioManager {

    private const val TAG = "AudioManager"

    private lateinit var context: Context
    private var mediaPlayer: MediaPlayer? = null
    private val musicFiles = mutableListOf<String>()
    private var isMusicOn = true
    private var volume = 0.5f
    private val random = Random()
    private var currentIndex = -1
    private var isPausedByLifecycle = false
    private var isPreparing = false

    @JvmStatic
    fun init(context: Context) {
        this.context = context.applicationContext
        loadMusicList()
        if (musicFiles.isNotEmpty() && mediaPlayer == null && !isPreparing) {
            Handler(Looper.getMainLooper()).postDelayed({
                if (mediaPlayer == null && !isPreparing) {
                    playRandom()
                }
            }, 500)
        } else {
            Log.w(TAG, "未找到任何音乐文件，请检查 assets/music/ 目录")
        }
    }

    private fun loadMusicList() {
        try {
            val files = context.assets.list("music")
            if (files != null && files.isNotEmpty()) {
                for (file in files) {
                    if (file.lowercase().endsWith(".ogg")) {
                        musicFiles.add("music/$file")
                    }
                }
            } else {
                Log.w(TAG, "music 目录为空或不存在")
            }
        } catch (e: IOException) {
            Log.e(TAG, "无法列出 assets/music/ 目录", e)
        }
    }

    @JvmStatic
    fun playRandom() {
        if (musicFiles.isEmpty()) return
        if (!isMusicOn) return
        if (isPreparing) return

        var index: Int
        do {
            index = random.nextInt(musicFiles.size)
        } while (index == currentIndex && musicFiles.size > 1)
        currentIndex = index
        play(musicFiles[index])
    }

    private fun play(assetPath: String) {
        stop()
        if (!isMusicOn) return
        isPreparing = true
        try {
            mediaPlayer = MediaPlayer().apply {
                val afd = context.assets.openFd(assetPath)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                setVolume(volume, volume)
                setOnPreparedListener { mp ->
                    isPreparing = false
                    mp.start()
                    setOnCompletionListener {
                        it.release()
                        mediaPlayer = null
                        isPreparing = false
                        playRandom()
                    }
                }
                setOnErrorListener { mp, what, extra ->
                    isPreparing = false
                    Log.e(TAG, "播放出错: what=$what extra=$extra")
                    mp.release()
                    mediaPlayer = null
                    Handler(Looper.getMainLooper()).postDelayed({
                        playRandom()
                    }, 1000)
                    true
                }
                prepareAsync()
            }
        } catch (e: IOException) {
            isPreparing = false
            Log.e(TAG, "无法播放: $assetPath", e)
        }
    }

    @JvmStatic
    fun stop() {
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
                it.release()
            } catch (e: Exception) {
                Log.e(TAG, "停止播放异常", e)
            }
            mediaPlayer = null
        }
        isPreparing = false
    }

    @JvmStatic
    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                isPausedByLifecycle = true
            }
        }
    }

    @JvmStatic
    fun resume() {
        if (isPausedByLifecycle && isMusicOn) {
            mediaPlayer?.let {
                try {
                    it.start()
                    isPausedByLifecycle = false
                } catch (e: Exception) {
                    it.release()
                    mediaPlayer = null
                    playRandom()
                }
            } ?: run {
                isPausedByLifecycle = false
                playRandom()
            }
        }
    }

    @JvmStatic
    fun setMusicOn(on: Boolean) {
        isMusicOn = on
        if (on) {
            if (mediaPlayer == null && !isPreparing) {
                playRandom()
            } else if (mediaPlayer?.isPlaying == false && !isPausedByLifecycle) {
                mediaPlayer?.start()
            }
        } else {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                }
            }
        }
    }

    @JvmStatic
    fun isMusicOn(): Boolean = isMusicOn

    @JvmStatic
    fun setVolume(vol: Float) {
        volume = vol.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(volume, volume)
    }

    @JvmStatic
    fun getVolume(): Float = volume

    @JvmStatic
    fun release() {
        stop()
    }
}
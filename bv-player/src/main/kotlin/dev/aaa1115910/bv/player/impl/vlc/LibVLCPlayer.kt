package dev.aaa1115910.bv.player.impl.vlc

import android.content.Context
import android.net.Uri
import dev.aaa1115910.bv.player.AbstractVideoPlayer
import dev.aaa1115910.bv.player.VideoPlayerOptions
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer

class LibVLCPlayer(
    private val context: Context,
    private val options: VideoPlayerOptions
) : AbstractVideoPlayer(), MediaPlayer.EventListener {
    var libVlc: LibVLC? = null
    var mediaPlayer: MediaPlayer? = null

    init {
        initPlayer()
    }

    override fun initPlayer() {
        val vlcOptions = ArrayList<String>()
        val hardwareDecoding = if (options.enableSoftwareVideoDecoder) 0 else -1
        vlcOptions.add("--avcodec-hw=$hardwareDecoding")
        vlcOptions.add("--rtsp-tcp")
        // 从 options 读取统一缓冲配置（秒转换为毫秒）
        val networkCaching = options.bufferSeconds * 1000 // 将秒转换为毫秒
        vlcOptions.add("--network-caching=$networkCaching")
        // 如果是直播卡顿，还需要加这个：
        vlcOptions.add("--live-caching=$networkCaching")
        libVlc = LibVLC(context, vlcOptions)
        mediaPlayer = MediaPlayer(libVlc)
        mediaPlayer?.setEventListener(this)
    }

    override fun setHeader(headers: Map<String, String>) {
    }

    override fun playUrl(videoUrl: String?, audioUrl: String?) {
        if (videoUrl == null) return
        val uri = Uri.parse(videoUrl)
        val media = Media(libVlc, uri)

        options.userAgent?.let { media.addOption(":http-user-agent=$it") }
        options.referer?.let { media.addOption(":http-referrer=$it") }

        if (audioUrl != null) {
            media.addSlave(org.videolan.libvlc.interfaces.IMedia.Slave(org.videolan.libvlc.interfaces.IMedia.Slave.Type.Audio, 4, audioUrl))
        }

        mediaPlayer?.media = media
        media.release()
    }

    override fun prepare() {
        mediaPlayer?.play()
    }

    override fun start() {
        mediaPlayer?.play()
    }

    override fun pause() {
        mediaPlayer?.pause()
    }

    override fun stop() {
        mediaPlayer?.stop()
    }

    override fun reset() {
        mediaPlayer?.stop()
        mediaPlayer?.media = null
    }

    override val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying == true

    override fun seekTo(time: Long) {
        if (mediaPlayer?.isSeekable == true) {
            mediaPlayer?.time = time
        }
    }

    override fun release() {
        mediaPlayer?.setEventListener(null)
        mediaPlayer?.release()
        libVlc?.release()
        mediaPlayer = null
        libVlc = null
    }

    override val currentPosition: Long
        get() = mediaPlayer?.time ?: 0

    override val duration: Long
        get() = mediaPlayer?.length ?: 0

    override val bufferedPercentage: Int
        get() = 0

    override fun setOptions() {
    }

    override var speed: Float
        get() = mediaPlayer?.rate ?: 1f
        set(value) {
            mediaPlayer?.rate = value
        }

    override val tcpSpeed: Long
        get() = 0

    override val debugInfo: String
        get() = "LibVLC"

    override val videoWidth: Int
        get() = 0

    override val videoHeight: Int
        get() = 0

    override fun onEvent(event: MediaPlayer.Event?) {
        when (event?.type) {
            MediaPlayer.Event.Buffering -> {
                if (event.buffering == 100f) {
                    mPlayerEventListener?.onReady()
                    mPlayerEventListener?.onPlay()
                } else {
                    mPlayerEventListener?.onBuffering()
                }
            }

            MediaPlayer.Event.Playing -> mPlayerEventListener?.onPlay()
            MediaPlayer.Event.Paused -> mPlayerEventListener?.onPause()
            MediaPlayer.Event.EncounteredError -> mPlayerEventListener?.onError(Exception("LibVLC Error"))
            MediaPlayer.Event.EndReached -> mPlayerEventListener?.onEnd()
        }
    }
}

package com.example.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class AudioToneSynthesizer(private val context: Context) {
    private var audioTrack: AudioTrack? = null
    private var fallbackRingtone: Ringtone? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    @Volatile
    private var isPlaying = false

    @Volatile
    private var currentVolume = 0.8f

    fun startProfile(
        profile: SoundProfile,
        volume: Float = 0.8f,
        progressiveRampMinutes: Int = 0
    ) {
        stop()
        currentVolume = if (progressiveRampMinutes > 0) 0.1f else volume
        isPlaying = true

        playbackJob = scope.launch {
            try {
                playSynthesizedSound(profile, volume, progressiveRampMinutes)
            } catch (e: Exception) {
                e.printStackTrace()
                playFallbackRingtone()
            }
        }
    }

    private suspend fun playSynthesizedSound(
        profile: SoundProfile,
        targetVolume: Float,
        rampMinutes: Int
    ) {
        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_OUT_STEREO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(4096)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack = track
        track.play()

        val fLeft = profile.carrierFreqHz
        val fRight = if (profile.beatFreqHz > 0) profile.carrierFreqHz + profile.beatFreqHz else profile.carrierFreqHz

        var phaseLeft = 0.0
        var phaseRight = 0.0
        val phaseLeftInc = 2.0 * PI * fLeft / sampleRate
        val phaseRightInc = 2.0 * PI * fRight / sampleRate

        val shortBuffer = ShortArray(bufferSize)
        val rampSteps = if (rampMinutes > 0) rampMinutes * 60 * 10 else 1 // step every 100ms
        var currentStep = 0
        var sampleCount = 0

        while (scope.isActive && isPlaying) {
            // Volume ramp logic
            if (rampMinutes > 0 && currentStep < rampSteps) {
                sampleCount += bufferSize / 2
                if (sampleCount >= sampleRate / 10) { // every 100ms
                    sampleCount = 0
                    currentStep++
                    val progress = (currentStep.toFloat() / rampSteps).coerceIn(0f, 1f)
                    currentVolume = 0.1f + (targetVolume - 0.1f) * progress
                }
            } else {
                currentVolume = targetVolume
            }

            for (i in 0 until bufferSize step 2) {
                var sampleL: Double
                var sampleR: Double

                when (profile.category) {
                    SoundCategory.BINAURAL_BEAT -> {
                        // Left & Right slight frequency offset
                        sampleL = sin(phaseLeft)
                        sampleR = sin(phaseRight)
                    }
                    SoundCategory.ZEN_MEDITATION -> {
                        // Tibetan singing bowl rich harmonics (1st, 2nd, 3rd partials)
                        val fundamental = sin(phaseLeft)
                        val overtone1 = 0.5 * sin(phaseLeft * 2.76)
                        val overtone2 = 0.25 * sin(phaseLeft * 5.4)
                        sampleL = (fundamental + overtone1 + overtone2) / 1.75
                        sampleR = sampleL
                    }
                    SoundCategory.GENTLE_NATURE -> {
                        // Gentle ambient chord swell
                        val pulse = 0.5 * (1.0 + sin(2.0 * PI * (profile.beatFreqHz) * phaseLeft / (2.0 * PI * fLeft)))
                        val harmonic = sin(phaseLeft) + 0.3 * sin(phaseLeft * 1.5)
                        sampleL = harmonic * pulse
                        sampleR = sampleL
                    }
                    SoundCategory.ENERGETIC -> {
                        // Rhythmic 120BPM pulse wave
                        val pulseMod = if ((phaseLeft % (2.0 * PI * 4)) < PI * 2) 1.0 else 0.2
                        sampleL = sin(phaseLeft) * pulseMod
                        sampleR = sampleL
                    }
                    SoundCategory.CLASSIC_ALARM -> {
                        // Double beep chime cadence
                        val cadence = ((phaseLeft / (2.0 * PI * 10)) % 2.0)
                        val isBeeping = cadence < 0.8
                        sampleL = if (isBeeping) sin(phaseLeft) else 0.0
                        sampleR = sampleL
                    }
                }

                phaseLeft = (phaseLeft + phaseLeftInc) % (2.0 * PI)
                phaseRight = (phaseRight + phaseRightInc) % (2.0 * PI)

                val scaledL = (sampleL * currentVolume * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                val scaledR = (sampleR * currentVolume * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())

                shortBuffer[i] = scaledL.toShort()
                shortBuffer[i + 1] = scaledR.toShort()
            }

            track.write(shortBuffer, 0, bufferSize)
        }

        try {
            track.stop()
            track.release()
        } catch (_: Exception) {}
        audioTrack = null
    }

    private fun playFallbackRingtone() {
        try {
            val alert: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, alert)
            fallbackRingtone = ringtone
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        isPlaying = false
        playbackJob?.cancel()
        playbackJob = null

        try {
            audioTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.release()
            }
        } catch (_: Exception) {}
        audioTrack = null

        try {
            fallbackRingtone?.stop()
        } catch (_: Exception) {}
        fallbackRingtone = null
    }

    fun isCurrentlyPlaying(): Boolean = isPlaying
}

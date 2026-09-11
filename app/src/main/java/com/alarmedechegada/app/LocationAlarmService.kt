package com.alarmedechegada.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat

class LocationAlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var vibrator: Vibrator? = null

    companion object {
        const val CHANNEL_ID = "alarme_chegada_channel"
        const val NOTIFICATION_ID = 43
        var tocando = false
            private set
        var nomeLocalAtual: String = ""
            private set

        fun pararAlarme(context: Context) {
            context.stopService(Intent(context, LocationAlarmService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        criarCanalNotificacao()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        tocando = true
        nomeLocalAtual = intent?.getStringExtra("nome_local") ?: "seu destino"
        iniciarWakeLock()
        iniciarSom()
        iniciarVibracao()
        startForeground(NOTIFICATION_ID, criarNotificacao())

        val activityIntent = Intent(this, LocationAlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(activityIntent)

        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (tocando) {
            val activityIntent = Intent(this, LocationAlarmActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(activityIntent)
        }
    }

    override fun onDestroy() {
        tocando = false
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun iniciarWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "AlarmeChegada::AlarmWakeLock"
        )
        wakeLock?.acquire(10 * 60 * 1000L)
    }

    private fun iniciarSom() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(
            AudioManager.STREAM_ALARM,
            audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
            0
        )

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(resources.openRawResourceFd(R.raw.alarme_som))
            isLooping = true
            prepare()
            start()
        }
    }

    private fun iniciarVibracao() {
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val padrao = longArrayOf(0, 800, 400)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(padrao, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(padrao, 0)
        }
    }

    private fun criarCanalNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Alarme de Chegada", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal do Alarme de Chegada"
                setBypassDnd(true)
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun criarNotificacao(): android.app.Notification {
        val fullScreenIntent = Intent(this, LocationAlarmActivity::class.java)
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_local)
            .setContentTitle("Você chegou!")
            .setContentText("Chegando em $nomeLocalAtual")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }
}

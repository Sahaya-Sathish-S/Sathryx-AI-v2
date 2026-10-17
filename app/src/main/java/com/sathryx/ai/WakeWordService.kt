package com.sathryx.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Experimental wake phrase detector using Android's SpeechRecognizer.
 * This is not a dedicated low-power wake-word engine. Provider behavior,
 * silence timeouts, offline availability, and repeated sessions vary by device.
 */
class WakeWordService : Service() {
    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private var restarting = false

    companion object {
        private const val CHANNEL_ID = "sathryx_listener"
        private const val NOTIFICATION_ID = 902
        private const val TAG = "SathryxWake"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Listener starting"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            updateNotification("Speech recognition unavailable")
            stopSelf()
            return START_NOT_STICKY
        }
        startRecognitionSession()
        return START_STICKY
    }

    private fun startRecognitionSession() {
        if (restarting) return
        restarting = true
        try {
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(this)
            recognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    listening = true
                    updateNotification("Listening for “Hey Sathryx”")
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { listening = false }
                override fun onError(error: Int) {
                    listening = false
                    Log.d(TAG, "Recognition error=$error")
                    // Avoid a tight retry loop. Stop after provider errors; user can restart from the app.
                    updateNotification("Listener paused (speech provider error $error)")
                    stopSelf()
                }
                override fun onResults(results: Bundle?) {
                    listening = false
                    val phrases = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                    val heardWakeWord = phrases.any { phrase ->
                        val normalized = phrase.lowercase()
                            .replace(Regex("[^a-z0-9 ]"), " ")
                            .replace(Regex("\\s+"), " ").trim()
                        normalized.contains("hey sathryx") ||
                        normalized.contains("hey satrics") ||
                        normalized.contains("hey sat ricks")
                    }
                    if (heardWakeWord) {
                        updateNotification("Wake phrase detected — open Sathryx to continue")
                        val openApp = packageManager.getLaunchIntentForPackage(packageName)
                        if (openApp != null) {
                            openApp.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            try { startActivity(openApp) } catch (e: Exception) {
                                Log.w(TAG, "Could not open activity from service", e)
                            }
                        }
                        stopSelf()
                    } else {
                        // This experimental version listens for one recognition result, then stops.
                        // User can restart the service from the app to listen again.
                        updateNotification("No wake phrase detected — listener stopped")
                        stopSelf()
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
            recognizer?.startListening(speechIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Could not start recognition", e)
            updateNotification("Could not start listener")
            stopSelf()
        } finally {
            restarting = false
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Sathryx wake-word listener", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Visible notification while Sathryx uses microphone recognition"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(status: String): Notification {
        val openIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pending = openIntent?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0))
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("SATHRYX AI")
                .setContentText(status)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .setContentIntent(pending)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("SATHRYX AI")
                .setContentText(status)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .setContentIntent(pending)
                .build()
        }
    }

    private fun updateNotification(status: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(status))
    }

    override fun onDestroy() {
        listening = false
        try {
            recognizer?.cancel()
            recognizer?.destroy()
        } catch (_: Exception) {}
        recognizer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

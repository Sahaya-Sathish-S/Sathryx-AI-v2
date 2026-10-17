package com.sathryx.ai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var output: TextView
    private lateinit var commandInput: EditText
    private lateinit var tts: TextToSpeech
    private var pendingAction: String? = null
    private var pendingPackage: String? = null
    private var ttsReady = false

    companion object {
        private const val REQ_SPEECH = 501
        private const val REQ_PERMISSIONS = 502
        private const val ACTION_YOUTUBE = "youtube"
        private const val ACTION_WHATSAPP = "whatsapp"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        buildUi()
        requestAppPermissionsIfNeeded()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 26, 22, 22)
            setBackgroundColor(0xFF080D14.toInt())
        }

        val title = TextView(this).apply {
            text = "S A T H R Y X"
            textSize = 25f
            gravity = Gravity.CENTER
            setTextColor(0xFF83EAF3.toInt())
            setPadding(0, 8, 0, 4)
        }
        val subtitle = TextView(this).apply {
            text = "PERSONAL AI COMPANION • V2"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(0xFF9AABB8.toInt())
            setPadding(0, 0, 0, 20)
        }
        output = TextView(this).apply {
            text = "Vanakkam! I'm Sathryx. Type or speak a command.\n\nTry: “Open YouTube” or “Open WhatsApp”. I will ask before launching."
            textSize = 16f
            setTextColor(0xFFEAF5FA.toInt())
            setPadding(16, 18, 16, 18)
            setBackgroundColor(0xFF121C28.toInt())
        }
        val scroll = ScrollView(this).apply {
            addView(output)
        }
        commandInput = EditText(this).apply {
            hint = "Speak or type a command…"
            setHintTextColor(0xFF718493.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 16f
            setPadding(12, 12, 12, 12)
        }

        val send = makeButton("Check command") {
            handleCommand(commandInput.text.toString())
        }
        val speak = makeButton("🎙 Speak") { startSpeechInput() }
        val startWake = makeButton("Start “Hey Sathryx” listener") { startWakeService() }
        val stopWake = makeButton("Stop wake-word listener") { stopService(Intent(this, WakeWordService::class.java)) }

        root.addView(title)
        root.addView(subtitle)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(commandInput, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 12 })
        root.addView(send)
        root.addView(speak)
        root.addView(startWake)
        root.addView(stopWake)
        setContentView(root)
    }

    private fun makeButton(label: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { action() }
        }
    }

    private fun requestAppPermissionsIfNeeded() {
        val permissions = mutableListOf<String>()
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (permissions.isNotEmpty()) requestPermissions(permissions.toTypedArray(), REQ_PERMISSIONS)
    }

    private fun startSpeechInput() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestAppPermissionsIfNeeded()
            say("Please grant microphone permission first.")
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Sathryx in English or Tamil")
        }
        try {
            startActivityForResult(intent, REQ_SPEECH)
        } catch (_: Exception) {
            say("Speech recognition is not available on this phone. You can type the command.")
        }
    }

    @Deprecated("Deprecated by Android; kept for compatibility with this simple starter project")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_SPEECH && resultCode == RESULT_OK) {
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = results?.firstOrNull().orEmpty()
            commandInput.setText(spoken)
            handleCommand(spoken)
        }
    }

    private fun handleCommand(raw: String) {
        val text = raw.trim()
        if (text.isEmpty()) {
            say("Please tell me what you want me to do.")
            return
        }
        val lower = text.lowercase(Locale.ROOT)
        val action = when {
            (lower.contains("open") || lower.contains("launch") || lower.contains("start") ||
             lower.contains("thira") || lower.contains("திற")) && lower.contains("youtube") -> ACTION_YOUTUBE
            (lower.contains("open") || lower.contains("launch") || lower.contains("start") ||
             lower.contains("திற")) && lower.contains("whatsapp") -> ACTION_WHATSAPP
            lower.contains("hey sathryx") -> {
                say("I'm here. Tell me what you need.")
                return
            }
            else -> null
        }

        if (action == null) {
            output.text = "You said:\n$text\n\nV2 currently supports approval-gated YouTube and WhatsApp launching. Other commands are not executed yet."
            say("I heard you. This action is not supported yet.")
            return
        }

        pendingAction = action
        pendingPackage = if (action == ACTION_YOUTUBE) "com.google.android.youtube" else "com.whatsapp"
        val display = if (action == ACTION_YOUTUBE) "Open YouTube" else "Open WhatsApp"
        output.text = "PERMISSION REQUIRED\n\nSathryx wants to: $display\n\nNothing will happen until you approve.\n\nApprove only if this is what you intended."
        showApprovalDialog(display)
    }

    private fun showApprovalDialog(display: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Sathryx permission check")
            .setMessage("May I $display?\n\nNo action will occur unless you tap Approve.")
            .setPositiveButton("Approve") { _, _ -> executePendingAction() }
            .setNegativeButton("Reject") { _, _ ->
                pendingAction = null
                pendingPackage = null
                output.text = "Action rejected. Nothing was launched."
                say("Okay, I cancelled that action.")
            }
            .setCancelable(false)
            .show()
    }

    private fun executePendingAction() {
        val action = pendingAction
        val packageName = pendingPackage
        pendingAction = null
        pendingPackage = null
        if (action == null || packageName == null) {
            say("There is no pending action.")
            return
        }
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        if (launch == null) {
            output.text = "The requested app is not installed or Android did not provide a launch intent."
            say("I could not find that app on your phone.")
            return
        }
        output.text = "Approved. Launching ${if (action == ACTION_YOUTUBE) "YouTube" else "WhatsApp"}."
        try {
            startActivity(launch)
        } catch (_: Exception) {
            output.text = "Android could not launch the app. You can open it manually."
            say("I couldn't launch that app.")
        }
    }

    private fun startWakeService() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestAppPermissionsIfNeeded()
            say("Please grant microphone permission first.")
            return
        }
        try {
            val intent = Intent(this, WakeWordService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
            output.text = "Wake-word service requested.\n\nKeep the persistent notification visible. Detection depends on your Android speech-recognition provider and may stop or restart on some devices."
            say("Wake word listener started. Say Hey Sathryx.")
        } catch (e: Exception) {
            output.text = "Could not start listener: ${e.message}\n\nAndroid may restrict microphone foreground services. Start it while the app is visible and check permissions."
        }
    }

    private fun say(text: String) {
        if (ttsReady) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sathryx-${System.currentTimeMillis()}")
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) {
            val result = tts.setLanguage(Locale("en", "IN"))
            tts.setSpeechRate(0.95f)
            tts.setPitch(1.08f)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.US)
            }
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) tts.shutdown()
        super.onDestroy()
    }
}

SATHRYX AI v2 — Native Android starter
======================================

This ZIP contains the Kotlin/Android project source. It does not contain a prebuilt APK.
Build the APK on your Android phone with AndroidIDE (recommended) or another Android Gradle IDE.

FEATURES
- Native Android Activity UI.
- Typed and Android-recognized voice commands.
- Explicit approval dialog before launching YouTube or WhatsApp.
- Experimental microphone foreground service that asks Android SpeechRecognizer to recognize one phrase and looks for “Hey Sathryx”.
- Female-sounding voice settings where the installed Android TTS voice supports them.

IMPORTANT WAKE-WORD LIMITATION
This is NOT a true low-power, always-on wake-word engine. Android SpeechRecognizer is designed for speech recognition sessions and may stop after one phrase, return provider errors, require network access, or behave differently by device. This starter intentionally stops after a result/error instead of looping indefinitely or silently restarting. For robust continuous wake-word detection, integrate a dedicated engine such as Porcupine with its SDK/access key and keyword model, or a suitable on-device wake-word model. Always-on listening after lock/reboot must be tested against your Android version, OEM restrictions, foreground-service rules, and battery settings.

PHONE-ONLY BUILD (AndroidIDE)
1. Install AndroidIDE from its official project sources and follow its current setup instructions:
   https://github.com/AndroidIDEOfficial/AndroidIDE
   AndroidIDE's setup UI and requirements may change. Install the SDK/build tools it requests.
2. Extract this ZIP in a folder such as Download/SATHRYX_AI_v2.
3. Open AndroidIDE and choose Open project. Select the extracted folder containing settings.gradle.kts.
4. Let Gradle sync. If it asks to install SDK Platform 35/build tools, accept the SDK components requested by the IDE.
5. Build > Assemble Project (or the equivalent Build APK action in your version of AndroidIDE).
6. Locate the generated debug APK under:
   app/build/outputs/apk/debug/app-debug.apk
7. Install the APK. Android may ask you to allow installs from that file manager/IDE. Only install the APK you built from this source.

FIRST RUN
1. Open SATHRYX AI.
2. Grant microphone permission when prompted.
3. Tap Speak, then say “Open YouTube” or type it.
4. Read the approval dialog. Tap Reject first to verify nothing launches; try again and tap Approve to launch.
5. Tap Start “Hey Sathryx” listener. Keep the persistent notification visible and say “Hey Sathryx” clearly. If it detects the phrase, it attempts to bring the app forward. This may not work on every device/provider.
6. Tap Stop wake-word listener to stop the service (or stop it from Android Settings > Apps > SATHRYX AI > Force stop).

SUPPORTED COMMANDS
- “Open YouTube”
- “Open WhatsApp”
The current command matcher intentionally supports only these launch actions. It does not send messages, make calls, change settings, delete files, or run arbitrary commands.

TAMIL/TANGLISH NOTE
The speech button currently requests en-IN recognition for broad device compatibility. Recognition of Tamil/Tanglish varies by installed speech provider. You can change the recognition locale in MainActivity.kt to "ta-IN", but then English recognition may be worse. A future version should include a language selector and separate command parsing.

VOICE NOTE
Android TTS voice gender and quality depend on the installed engine and voice packs. setPitch(1.08) does not guarantee a female voice. Install a preferred Tamil/English voice under Android Text-to-Speech settings.

BUILD TROUBLESHOOTING
- Gradle sync fails: check AndroidIDE's supported Gradle/JDK/SDK combination and install the SDK packages it requests. Avoid randomly upgrading plugin versions.
- “SDK location not found”: complete AndroidIDE's SDK setup or set its SDK path using its UI.
- Service start is blocked: start it from the visible activity, grant microphone permission, and allow notifications. Android 14+ has strict microphone foreground-service rules.
- Wake listener ends immediately: this is expected on some SpeechRecognizer providers; it is not a dedicated wake-word engine.
- App not found: install YouTube/WhatsApp or test the other supported app.
- For code edits, rebuild and reinstall the APK.

SECURITY
- The app does not request Accessibility Service, contacts, SMS, call log, location, or notification access in this version.
- App launching is approval-gated, but the app cannot verify the exact external app screen that appears after launch.
- Microphone listening is intended to be visible through the foreground-service notification. Stop it when not needed.
- Do not use wake-word listening in private settings where others expect privacy.

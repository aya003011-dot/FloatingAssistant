package com.example.floatingassistant;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Locale;

public class VoiceCommandActivity extends Activity {
    private static final int REQ_AUDIO = 501;
    private SpeechRecognizer recognizer;
    private TextView status;
    private TextView transcript;
    private Button recordButton;
    private boolean recording = false;
    private boolean listening = false;
    private boolean finishRequested = false;
    private boolean commandSubmitted = false;
    private String latestTranscript = "";
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildRecordingUi();
        recordButton.setOnClickListener(v -> {
            if (!recording) startRecording();
            else stopAndSubmit();
        });
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
        }
    }

    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }

    private void buildRecordingUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(32), dp(24), dp(24));
        root.setBackgroundColor(0xFF20232B);

        TextView title = new TextView(this);
        title.setText("فرمان صوتی");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        status = new TextView(this);
        status.setText("برای شروع، دکمهٔ میکروفون رو بزن. یک بار دیگه بزنی، ضبط تموم می‌شه و فرمان اجرا می‌شه.");
        status.setTextColor(0xFFE0E0E0);
        status.setTextSize(16);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(26), 0, dp(20));
        root.addView(status, new LinearLayout.LayoutParams(-1, -2));

        transcript = new TextView(this);
        transcript.setText("متن تشخیص‌داده‌شده اینجا نمایش داده می‌شه.");
        transcript.setTextColor(0xFFFFFFFF);
        transcript.setTextSize(17);
        transcript.setGravity(Gravity.CENTER);
        transcript.setPadding(dp(12), dp(22), dp(12), dp(22));
        transcript.setBackgroundColor(0xFF303641);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, 0, 1);
        tp.bottomMargin = dp(20);
        root.addView(transcript, tp);

        recordButton = new Button(this);
        recordButton.setText("🎙 شروع ضبط");
        root.addView(recordButton, new LinearLayout.LayoutParams(-1, dp(58)));

        Button cancel = new Button(this);
        cancel.setText("لغو");
        cancel.setOnClickListener(v -> { finishRequested = true; releaseRecognizer(); finish(); });
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, dp(48));
        cp.topMargin = dp(10);
        root.addView(cancel, cp);
        setContentView(root);
    }

    private void startRecording() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.setText("تشخیص گفتار روی این گوشی در دسترس نیست. سرویس گفتار گوشی رو بررسی کن یا دستور رو تایپ کن.");
            return;
        }
        try {
            if (recognizer != null) releaseRecognizer();
            recognizer = SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) {
                    listening = true;
                    status.setText("در حال گوش‌دادن… وقتی حرفت تموم شد، دوباره روی دکمه بزن تا فرمان اجرا بشه.");
                }
                @Override public void onBeginningOfSpeech() {
                    status.setText("دارم گوش می‌دم… برای پایان و اجرا، دوباره روی دکمه بزن.");
                }
                @Override public void onRmsChanged(float rmsdB) { }
                @Override public void onBufferReceived(byte[] buffer) { }
                @Override public void onEndOfSpeech() {
                    listening = false;
                    status.setText("گفتار پایان یافت. برای اجرای فرمان روی «پایان ضبط و اجرا» بزن.");
                }
                @Override public void onError(int error) {
                    listening = false;
                    if (finishRequested) { finishWithCommand(); return; }
                    status.setText("ضبط متوقف شد. برای تلاش دوباره دکمه رو بزن.");
                    recording = false;
                    recordButton.setText("🎙 شروع ضبط");
                }
                @Override public void onResults(Bundle results) {
                    listening = false;
                    ArrayList<String> list = results == null ? null : results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (list != null && !list.isEmpty()) {
                        latestTranscript = list.get(0).trim();
                        transcript.setText(latestTranscript.isEmpty() ? "چیزی تشخیص داده نشد." : latestTranscript);
                    }
                    if (finishRequested) finishWithCommand();
                    else status.setText("متن تا اینجا آماده است. برای اجرای فرمان، روی دکمهٔ پایان بزن.");
                }
                @Override public void onPartialResults(Bundle partialResults) {
                    ArrayList<String> list = partialResults == null ? null : partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (list != null && !list.isEmpty()) {
                        String partial = list.get(0).trim();
                        if (!partial.isEmpty()) { latestTranscript = partial; transcript.setText(partial); }
                    }
                }
                @Override public void onEvent(int eventType, Bundle params) { }
            });
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR");
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fa-IR");
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            // Hint the installed speech provider to use its on-device model where available.
            // Providers may ignore this hint, so the README explains the language-pack requirement.
            intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
            latestTranscript = "";
            finishRequested = false;
            commandSubmitted = false;
            recording = true;
            recordButton.setText("■ پایان ضبط و اجرا");
            transcript.setText("...");
            recognizer.startListening(intent);
        } catch (Exception e) {
            listening = false;
            recording = false;
            recordButton.setText("🎙 شروع ضبط");
            status.setText("شروع ضبط ممکن نشد. می‌تونی دستور رو تایپ کنی.");
        }
    }

    private void stopAndSubmit() {
        recording = false;
        finishRequested = true;
        recordButton.setEnabled(false);
        recordButton.setText("در حال اجرا…");
        status.setText("در حال نهایی‌کردن متن و اجرای فرمان…");
        if (recognizer != null) {
            if (listening) {
                try { recognizer.stopListening(); } catch (Exception ignored) { }
            }
            // Give SpeechRecognizer time to return its final result; use the latest partial transcript if it does not.
            handler.postDelayed(this::finishWithCommand, 1400);
        } else {
            finishWithCommand();
        }
    }

    private void finishWithCommand() {
        if (!finishRequested || commandSubmitted) return;
        String command = latestTranscript == null ? "" : latestTranscript.trim();
        if (command.isEmpty()) {
            commandSubmitted = true;
            Toast.makeText(this, "فرمانی تشخیص داده نشد؛ دوباره ضبط کن.", Toast.LENGTH_LONG).show();
            releaseRecognizer();
            finish();
            return;
        }
        commandSubmitted = true;
        FloatingService.submitCommandFromVoice(command);
        releaseRecognizer();
        finish();
    }

    private void releaseRecognizer() {
        listening = false;
        if (recognizer != null) {
            try { recognizer.cancel(); } catch (Exception ignored) { }
            try { recognizer.destroy(); } catch (Exception ignored) { }
            recognizer = null;
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_AUDIO && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            status.setText("اجازهٔ میکروفون داده شد. برای شروع ضبط روی دکمه بزن.");
        } else {
            Toast.makeText(this, "برای شنیدن فرمان باید اجازهٔ میکروفون رو بدی.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        releaseRecognizer();
        super.onDestroy();
    }
}

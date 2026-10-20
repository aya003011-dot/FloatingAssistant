package com.example.floatingassistant;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Locale;

public class VoiceCommandActivity extends Activity {
    private static final int REQ_AUDIO = 501;
    private static final int REQ_SPEECH = 502;
    private boolean launched = false;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
        } else startRecognition();
    }
    private void startRecognition() {
        if (launched) return; launched = true;
        try {
            Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR");
            i.putExtra(RecognizerIntent.EXTRA_PROMPT, "دستورت رو بگو");
            startActivityForResult(i, REQ_SPEECH);
        } catch (Exception e) { Toast.makeText(this,"تشخیص گفتار روی این گوشی در دسترس نیست؛ دستور رو تایپ کن.",Toast.LENGTH_LONG).show(); finish(); }
    }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode==REQ_AUDIO && grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED) startRecognition();
        else { Toast.makeText(this,"برای شنیدن دستور باید اجازهٔ میکروفون بدی.",Toast.LENGTH_LONG).show(); finish(); }
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==REQ_SPEECH && resultCode==RESULT_OK && data!=null) {
            ArrayList<String> results=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if(results!=null&&!results.isEmpty()) FloatingService.submitCommandFromVoice(results.get(0));
        }
        finish();
    }
}

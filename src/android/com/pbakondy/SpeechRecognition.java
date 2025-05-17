package com.pbakondy;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;

import android.Manifest;
import android.content.Context;
import android.content.Intent;                  // ← 追加
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SpeechRecognition extends CordovaPlugin implements RecognitionListener {

    private static final String LOG_TAG               = "SpeechRecognition";
    private static final String START_LISTENING       = "startListening";
    private static final String STOP_LISTENING        = "stopListening";
    private static final String HAS_PERMISSION        = "hasPermission";
    private static final String REQUEST_PERMISSION    = "requestPermission";
    private static final String RECORD_AUDIO_PERMISSION = Manifest.permission.RECORD_AUDIO;

    private CallbackContext callbackContext;
    private SpeechRecognizer recognizer;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        // SpeechRecognizer を先に作成
        recognizer = SpeechRecognizer.createSpeechRecognizer(cordova.getActivity());
        recognizer.setRecognitionListener(this);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext ctx) throws JSONException {
        this.callbackContext = ctx;

        if (START_LISTENING.equals(action)) {
            // パーミッションチェック
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !cordova.hasPermission(RECORD_AUDIO_PERMISSION)) {
                ctx.error("Missing permission");
            } else {
                String lang       = args.optString(0, Locale.getDefault().toString());
                int maxResults    = args.optInt(1, 5);
                startListening(lang, maxResults);
                ctx.success();
            }
            return true;
        }

        if (STOP_LISTENING.equals(action)) {
            if (recognizer != null) recognizer.stopListening();
            ctx.success();
            return true;
        }

        if (HAS_PERMISSION.equals(action)) {
            boolean granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || cordova.hasPermission(RECORD_AUDIO_PERMISSION);
            ctx.success(granted ? 1 : 0);
            return true;
        }

        if (REQUEST_PERMISSION.equals(action)) {
            cordova.requestPermission(this, 12345, RECORD_AUDIO_PERMISSION);
            return true;
        }

        return false;
    }

    private void startListening(String language, int maxResults) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, maxResults);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        recognizer.startListening(intent);
    }

    // RecognitionListener の実装 ↓

    @Override
    public void onReadyForSpeech(Bundle params) { }

    @Override
    public void onBeginningOfSpeech() { }

    @Override
    public void onRmsChanged(float rmsdB) { }

    @Override
    public void onBufferReceived(byte[] buffer) { }

    @Override
    public void onEndOfSpeech() { }

    @Override
    public void onError(int error) {
        callbackContext.error(getErrorText(error));
    }

    @Override
    public void onResults(Bundle results) {
        ArrayList<String> matches = results.getStringArrayList(
            SpeechRecognizer.RESULTS_RECOGNITION);
        callbackContext.success(new JSONArray(matches));
    }

    @Override
    public void onPartialResults(Bundle partialResults) { }

    @Override
    public void onEvent(int eventType, Bundle params) { }

    // CordovaPlugin の onActivityResult は Intent を扱わないので super のみ
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults)
            throws JSONException {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            callbackContext.success();
        } else {
            callbackContext.error("Permission denied");
        }
    }

    // エラーコード→メッセージ
    private String getErrorText(int code) {
        switch (code) {
            case SpeechRecognizer.ERROR_AUDIO: return "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT: return "Client side error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "Insufficient permissions";
            case SpeechRecognizer.ERROR_NETWORK: return "Network error";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH: return "No match";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: return "Recognition service busy";
            case SpeechRecognizer.ERROR_SERVER: return "Server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "No speech input";
            default: return "Unknown error";
        }
    }
}

package com.hift.nameofthings;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.Locale;

/**
 * Created by TeguhAS on 07-Aug-17.
 */

public class WordSpeaker implements TextToSpeech.OnInitListener {
    private TextToSpeech tts;
    private String word;
    private Locale locale;

    public WordSpeaker(Context context, String word, Locale locale) {
        tts = new TextToSpeech(context.getApplicationContext(), this);
        tts.setOnUtteranceProgressListener(mUtteranceListener);
        this.word = word;
        this.locale = locale;
    }

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.ERROR) {
            tts.setLanguage(locale);
            tts.speak(word, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    public TextToSpeech getTts() {
        return this.tts;
    }

    private UtteranceProgressListener mUtteranceListener = new UtteranceProgressListener() {
        @Override
        public void onStart(String s) {
        }

        @Override
        public void onDone(String s) {//STOP AND SHUTDOWN TTS WHEN COMPLETED TALKING
            try {
                tts.stop();
                tts.shutdown();
            } catch (Exception ignore) {
            }
        }

        @Override
        public void onError(String s) {
        }
    };
}

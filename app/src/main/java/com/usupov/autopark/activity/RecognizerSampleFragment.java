package com.usupov.autopark.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import ru.yandex.speechkit.Error;
import ru.yandex.speechkit.Recognition;
import ru.yandex.speechkit.RecognitionHypothesis;
import ru.yandex.speechkit.Recognizer;
import ru.yandex.speechkit.RecognizerListener;
import ru.yandex.speechkit.SpeechKit;
import android.support.v4.content.FileProvider;

import com.usupov.autopark.R;
import com.usupov.autopark.service.SpeachRecogn;

import java.util.ArrayList;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/**
 * This file is a part of the samples for Yandex SpeechKit Mobile SDK.
 * <br/>
 * Version for Android © 2016 Yandex LLC.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <br/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class RecognizerSampleFragment extends Fragment implements RecognizerListener {
    private static final String API_KEY_FOR_TESTS_ONLY = "8a2fdc7a-fc0d-476f-bebe-5f048ac278ac";

    private static final int REQUEST_PERMISSION_CODE = 1;

    private ProgressBar progressBar;
    private TextView currentStatus;
    private TextView recognitionResult;
    private TextView textAll;

    private static ArrayList<String> all_results;

    private Recognizer recognizer;
    private static String resultText;
    private Button cancelBtn;

    public RecognizerSampleFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        resultText = "";
        SpeechKit.getInstance().configure(getContext(), API_KEY_FOR_TESTS_ONLY);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sample, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        createAndStartRecognizer();
        progressBar = (ProgressBar) view.findViewById(R.id.voice_power_bar);
//        currentStatus = (TextView) view.findViewById(R.id.current_state);
//        recognitionResult = (TextView) view.findViewById(R.id.result);
//        textAll = (TextView) view.findViewById(R.id.texts_all);
        all_results = new ArrayList<>();
        cancelBtn = (Button) view.findViewById(R.id.btn_cancel);

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().setResult(Activity.RESULT_CANCELED, null);
                getActivity().finish();
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        resetRecognizer();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != REQUEST_PERMISSION_CODE) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length == 1 && grantResults[0] == PERMISSION_GRANTED) {
            createAndStartRecognizer();
        } else {
            updateStatus("Record audio permission was not granted");
        }
    }

    private void resetRecognizer() {
        if (recognizer != null) {
            recognizer.cancel();
            recognizer = null;
        }
    }

    @Override
    public void onRecordingBegin(Recognizer recognizer) {
        updateStatus("Запись началось");
    }

    @Override
    public void onSpeechDetected(Recognizer recognizer) {
        updateStatus("Голос определено");
    }

    @Override
    public void onSpeechEnds(Recognizer recognizer) {
        updateStatus("Голос закончилось");
    }

    @Override
    public void onRecordingDone(Recognizer recognizer) {
        updateStatus("Запись закончилось");
    }

    @Override
    public void onSoundDataRecorded(Recognizer recognizer, byte[] bytes) {
    }

    @Override
    public void onPowerUpdated(Recognizer recognizer, float power) {
        updateProgress((int) (power * progressBar.getMax()));
    }

    @Override
    public void onPartialResults(Recognizer recognizer, Recognition recognition, boolean b) {
        RecognitionHypothesis[]h = recognition.getHypotheses();
//        String result = "";
//        for (int i = 0; i < h.length; i++) {
//            result += h[i].toString() + "\n";
//        }
//        textAll.setText(result);
//        updateStatus("Partial results " + recognition.getBestResultText());
    }

    @Override
    public void onRecognitionDone(Recognizer recognizer, Recognition recognition) {
        String curResult = recognition.getBestResultText();
//        updateResult(curResult);
        all_results.clear();
        for (RecognitionHypothesis r : recognition.getHypotheses()) {
            all_results.add(r.getNormalized());
        }
        updateProgress(0);
        Intent intent = new Intent();
        intent.putExtra("recognated_string", curResult);
        intent.putStringArrayListExtra("all_results", all_results);
        getActivity().setResult(Activity.RESULT_OK, intent);
        getActivity().finish();
    }

    @Override
    public void onError(Recognizer recognizer, ru.yandex.speechkit.Error error) {
        if (error.getCode() == Error.ERROR_CANCELED) {
            updateStatus("Cancelled");
            updateProgress(0);
        } else {
            updateStatus("Error occurred " + error.getString());
            resetRecognizer();
        }
    }

    private void createAndStartRecognizer() {
        final Context context = getContext();
        if (context == null) {
            return;
        }

        if (ContextCompat.checkSelfPermission(context, RECORD_AUDIO) != PERMISSION_GRANTED) {
            requestPermissions(new String[]{RECORD_AUDIO}, REQUEST_PERMISSION_CODE);
        } else {
            // Reset the current recognizer.
            resetRecognizer();
            // To create a new recognizer, specify the language, the model - a scope of recognition to get the most appropriate results,
            // set the listener to handle the recognition events.
            recognizer = Recognizer.create(Recognizer.Language.RUSSIAN, Recognizer.Model.NOTES, RecognizerSampleFragment.this);
            // Don't forget to call start on the created object.
            recognizer.start();
        }
    }

    private void updateResult(String text) {
        resultText += text;
//        recognitionResult.setText(resultText);
    }

    private void updateStatus(final String text) {
//        currentStatus.setText(text);
    }

    private void updateProgress(int progress) {
        progressBar.setProgress(progress);
    }
}

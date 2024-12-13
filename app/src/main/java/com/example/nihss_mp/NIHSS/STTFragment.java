package com.example.nihss_mp.NIHSS;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.nihss_mp.R;

import org.opencv.core.Core;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

public class STTFragment extends Fragment {

    // 엑티비티와 연결...
    NihssActivity activity;
    @Override public void onAttach(Context context) {super.onAttach(context); if (getActivity() instanceof NihssActivity) {activity = (NihssActivity) getActivity();}}

    TextView STT_Text;
    TextView STT_Speak;
    TextView STT_Comment;
    ImageView STT_Image;
    Intent intent;
    SpeechRecognizer speechRecognizer;
    boolean isTTSInProgress = false;
    String[] STT_Texts = {
            "낫 놓고 기역자도 모른다",
            "가는 말이 고와야 오는 말이 곱다",
            "까마귀 날자 배 떨어진다",
            "고래 싸움에 새우 등 터진다",
            "말 한마디에 천냥 빚도 갚는다",
            "티끌모아 태산",
            "작은 고추가 맵다",
            "쥐 구멍에도 볕들 날이 있다",
            "원숭이도 나무에서 떨어진다"};




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stt, container, false);

        STT_Text = view.findViewById(R.id.STT_Text);
        STT_Speak = view.findViewById(R.id.STT_Speak);
        STT_Comment = view.findViewById(R.id.STT_Comment);
        STT_Image = view.findViewById(R.id.STT_Image);

        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getActivity().getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");

        ControlText();

        // TTS 종료 후 STT 시작
        if (activity.STT_phase == 2) {
            activity.Speak(activity, STT_Comment, "Vibrate", () -> {
                isTTSInProgress = false;
                STT_Image.setImageTintList(ColorStateList.valueOf(Color.parseColor("#FFD146")));
                StartSTT();
                activity.ControlFragment(10, "Vibrate");
            });
        } else {
            activity.Speak(activity, STT_Text, "STT", () -> {
                isTTSInProgress = false;
                STT_Image.setImageTintList(ColorStateList.valueOf(Color.parseColor("#FFD146")));
                StartSTT();
                activity.ControlFragment(8, "STT");
            });
        }




        return view;
    }

    String oriText;
    String recognizedText;

    void CalculateSTT(Bundle bundle) {
        oriText = "";
        recognizedText = "";
        ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        StringBuilder viewText = new StringBuilder();
        for (String match : matches) {
            viewText.append(match).append(" ");
        }
        STT_Speak.setText(viewText.toString());

        switch (activity.STT_phase){
            case 0 : // 나이 비교 -> 나이 정보를 가져올 수 없으므로 일단 비교 X
                oriText = STT_Text.getText().toString().trim();
                recognizedText = STT_Speak.getText().toString().trim();
                break;
            case 1 : // 몇월달인지 비교
                Calendar calendar = Calendar.getInstance();
                int month = calendar.get(Calendar.MONTH) + 1; // MONTH는 0부터 시작하므로 1 더해야 함
                oriText = String.valueOf(month);
                Log.d("STT", "캘린더 : " + oriText);

                recognizedText = STT_Speak.getText().toString().trim();
                recognizedText = recognizedText.replaceAll("[^0-9]", ""); // 숫자가 아닌 문자를 제거
                Log.d("STT", "말한단어 : " + recognizedText);

                if (oriText.equals(recognizedText)) {
                    Toast.makeText(activity, "정답! NIHSS : " + NihssActivity.NIHSS_total_score, Toast.LENGTH_SHORT).show();
                } else {
                    NihssActivity.NIHSS_total_score++;
                    Toast.makeText(activity, "오류! NIHSS : " + NihssActivity.NIHSS_total_score, Toast.LENGTH_SHORT).show();
                } break;

            case 2 : // 문장과 비교
                oriText = STT_Text.getText().toString().trim();
                recognizedText = STT_Speak.getText().toString().trim();

                if (oriText.equals(recognizedText)) {
                    Toast.makeText(activity, "정답! NIHSS : " + NihssActivity.NIHSS_total_score, Toast.LENGTH_SHORT).show();
                } else {
                    NihssActivity.NIHSS_total_score++;
                    Toast.makeText(activity, "오류! NIHSS : " + NihssActivity.NIHSS_total_score, Toast.LENGTH_SHORT).show();
                } break;

        }


    }

    void ControlText() {
        switch (activity.STT_phase) {
            case 0:
                STT_Text.setText("나이가 어떻게 되시나요?");
                break;
            case 1:
                STT_Text.setText("오늘이 몇 월인가요?");
                break;
            case 2:
                STT_Comment.setText("다음 문장을 읽어주세요");
                //문장 중 하나 랜덤...
                Random random = new Random();
                int randomIndex = random.nextInt(STT_Texts.length);
                String randomText = STT_Texts[randomIndex];
                STT_Text.setText(randomText);
                break;

        }
    }


    Handler handler = new Handler(); // 핸들러 선언
    boolean recording = false; // recording 변수 선언

    void StartSTT() {
        if (isTTSInProgress) return;

        recording = true; // 녹음 시작 상태로 설정
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getActivity());
        speechRecognizer.setRecognitionListener(listener);


        // 타임아웃 핸들러 설정 (8초 후 타임아웃으로 처리/텍스트 길이가 긴건 좀 길게...)
        if (activity.STT_phase == 2) { handler.postDelayed(() -> {
            if (recording) {
                StopSTT();
            }
        }, 10 * 1000); } else { handler.postDelayed(() -> {
            if (recording) {
                StopSTT();
            }
        }, 8 * 1000); }

        speechRecognizer.startListening(intent);
    }


    void StopSTT() {
        activity.STT_phase++;
        recording = false; // 녹음 상태 해제
        STT_Image.setImageTintList(ColorStateList.valueOf(Color.parseColor("#D3D3D3")));
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            speechRecognizer.destroy();
        }
    }


    RecognitionListener listener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle bundle) {}

        @Override
        public void onBeginningOfSpeech() {}

        @Override
        public void onRmsChanged(float v) {}

        @Override
        public void onBufferReceived(byte[] bytes) {}

        @Override
        public void onEndOfSpeech() {}

        @Override
        public void onError(int error) {
            String message;
            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO:
                    message = "오디오 에러";
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    return;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    message = "퍼미션 없음";
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    message = "네트워크 에러";
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    message = "네트웍 타임아웃";
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    NihssActivity.NIHSS_total_score += 1; StopSTT();
                    Toast.makeText(activity, "말을 인식하지 못했습니다. NIHSS : " + NihssActivity.NIHSS_total_score, Toast.LENGTH_SHORT).show();
                    return;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    message = "RECOGNIZER가 바쁨";
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    message = "서버가 이상함";
                    break;
                default:
                    message = "알 수 없는 오류임";
                    break;
            }
            Toast.makeText(activity.getApplicationContext(), "에러 발생: " + message, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onResults(Bundle bundle) {
            recording = false; // 음성 인식 완료로 녹음 상태 해제
            handler.removeCallbacksAndMessages(null); // 타임아웃 핸들러 제거
            CalculateSTT(bundle);
            StopSTT();
        }

        @Override
        public void onPartialResults(Bundle bundle) {}

        @Override
        public void onEvent(int i, Bundle bundle) {}
    };
}
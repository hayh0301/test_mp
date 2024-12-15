package com.example.nihss_mp.NIHSS;

import static java.lang.String.valueOf;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.nihss_mp.MainActivity;
import com.example.nihss_mp.R;

import java.util.Locale;

public class NihssActivity extends AppCompatActivity {

    Handler handler = new Handler(); // 핸들러 선언
    static FragmentTransaction fragmentTransaction_nihss;
    public static int NIHSS_total_score;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_nihss);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        //초기화
        fragmentTransaction_nihss = getSupportFragmentManager().beginTransaction();
        NIHSS_total_score = 0;
        Progressbar_NIHSS = findViewById(R.id.Progressbar_NIHSS);

        STT_phase = 0;

        vib_phase = 0; // 양손(0 - 왼손 1 - 오른손)
        draw_phase = 0; // 양손(0 - 왼손 1 - 오른손)
        acc_phase = 0; // 양손(0 - 왼손 1 - 오른손)
        face_phase = 0; // 0 - 얼굴 1 - 눈

        //시작
        ChangeFragment("First");

    }


    // 현재 프레그먼트 실행시간. 그 후 다음 프레그먼트로 change...
    public void ControlFragment(int thissecond, String nextFragname){
        SetProgressbar(thissecond);
        handler.postDelayed(() -> {ChangeFragment(nextFragname);}, thissecond * 1000);
    }




    public int STT_phase = 0;
    public int vib_phase = 0; // 양손(0 - 왼손 1 - 오른손)
    public int draw_phase = 0; // 양손(0 - 왼손 1 - 오른손)
    public int acc_phase = 0; // 양손(0 - 왼손 1 - 오른손)
    public int face_phase = 0; // 0 - 얼굴 1 - 눈


    // fragment control
    public void ChangeFragment(String fragment_name){
        Fragment fragment = null;

        // 교체할 프레그먼트
        switch (fragment_name){
            case "First" : fragment = new FirstFragment(); break;
            case "Second" : fragment = new SecondFragment(); break;
            case "STT" : fragment = new STTFragment(); break;
            case "Vibrate" : fragment = new VibratorFragment(); break;
            case "EyeDetect" : fragment = new EyeDetectFragment(); break;
            case "EyeDetect2" : fragment = new EyeDetect2Fragment(); break;
            case "Draw" : fragment = new DrawFragment(); break;
            case "Accelerator" : fragment = new AcceleratorFragment(); break;
            case "FaceDetect" : fragment = new FaceDetectFragment(); break;
            //case "FaceDetect2" : fragment = new FaceDetect2Fragment(); break;
            case "End" :
                // Toast.makeText(NihssActivity.this,"end", Toast.LENGTH_SHORT).show();
                fragment = new ScoreFragment();
                // fragment = null;
                break; //종료
            case "null" :
                fragment = null;
                break;
        }

        // 프레그먼트 교체
        if (fragment != null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.FragFrame_NIHSS, fragment).commit();
        } else{
            Intent intent = new Intent(NihssActivity.this, MainActivity.class);
            startActivity(intent);
            //Toast.makeText(NihssActivity.this,"NIHSS total score = " + NIHSS_total_score, Toast.LENGTH_SHORT).show();
        }

    }

    ProgressBar Progressbar_NIHSS;
    int progressStatus = 0;

    // ProgressBar 애니메이션과 프래그먼트 변경 함수
    public void SetProgressbar(int FragmentRunTime) {
        // progressStatus 초기화
        progressStatus = 0;
        Progressbar_NIHSS.setProgress(0);

        // ValueAnimator로 ProgressBar 애니메이션 생성
        ValueAnimator animator = ValueAnimator.ofInt(0, 100);  // 0%에서 100%까지 애니메이션
        animator.setDuration(FragmentRunTime * 1000);  // FragmentRunTime 초 동안 애니메이션 실행
        animator.setInterpolator(new LinearInterpolator());  // 일정한 속도로 애니메이션

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int animatedValue = (int) animation.getAnimatedValue();
                Progressbar_NIHSS.setProgress(animatedValue);  // ProgressBar를 애니메이션에 따라 업데이트
            }
        });

        // 애니메이션 시작
        animator.start();
    }




    private TextToSpeech TTS;
    private boolean isTTSSpeaking = false;

    public void Speak(Context context, TextView TTStext, String TTSnum, Runnable onTTSDoneCallback) {
        TTS = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if (i == TextToSpeech.SUCCESS) {
                    TTS.setLanguage(Locale.KOREAN);
                    TTS.setSpeechRate(1.5f);
                    TTS.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {
                            isTTSSpeaking = true;
                        }

                        @Override
                        public void onDone(String utteranceId) {
                            isTTSSpeaking = false;
                            if (utteranceId.equals(TTSnum)) {

                                if (context instanceof Activity) {
                                    ((Activity) context).runOnUiThread(() -> {
                                        TTS.shutdown();
                                        if (onTTSDoneCallback != null) {
                                            onTTSDoneCallback.run();
                                        }
                                    });
                                }
                            }
                        }

                        @Override
                        public void onError(String utteranceId) {
                            // Handle error
                        }
                    });
                    TTS.speak(TTStext.getText().toString(), TextToSpeech.QUEUE_FLUSH, null, TTSnum);
                }
            }
        });
    }

//    public boolean isTTSSpeaking() {
//        return isTTSSpeaking;
//    }


}
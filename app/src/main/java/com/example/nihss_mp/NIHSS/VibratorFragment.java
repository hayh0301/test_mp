package com.example.nihss_mp.NIHSS;

import android.content.Context;
import android.graphics.Color;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.nihss_mp.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VibratorFragment extends Fragment {

    // 엑티비티와 연결...
    NihssActivity activity;
    @Override public void onAttach(Context context) {super.onAttach(context); if (getActivity() instanceof NihssActivity) {activity = (NihssActivity) getActivity();}}

    FrameLayout Vibrator_view1;
    FrameLayout Vibrator_view2;

    TextView Vibrator_Comment, Vibrator_Comment2, vib;
    Button btn, btn2;
    Handler handler = new Handler();
    SoundPool soundPool; private int soundId;

    boolean BtnPress, isVibrate, correct;
    int round, random_round, score;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_vibrator, container, false);
        Vibrator_Comment = view.findViewById(R.id.Vibrator_Comment);
        Vibrator_Comment2 = view.findViewById(R.id.Vibrator_Comment2);
        activity.Progressbar_NIHSS.setVisibility(View.GONE);

        vib = view.findViewById(R.id.vib);
        vib.setVisibility(View.INVISIBLE);

        Vibrator_view1 = view.findViewById(R.id.Vibrator_view1);
        Vibrator_view2 = view.findViewById(R.id.Vibrator_view2);

        // 사운드
        soundPool = new SoundPool.Builder().setMaxStreams(1).build();
        soundId = soundPool.load(getContext(), R.raw.button2, 1);

        btn = view.findViewById(R.id.Vibrator_btn);
        btn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: // 버튼 누름
                        btn.setBackgroundColor(Color.parseColor("#D3D3D3")); // 누르는 동안 회색으로 바뀜
                        BtnPress = true;
                        // 1초 동안 누르면 진동 중단
                        handler.postDelayed(() -> {
                            if (BtnPress && isVibrate) { // 1초 동안 누르는 것 성공 && 진동 중
                                correct = true; // 정답
                                StopVibrate(); // 진동 중지
                                BtnPress = false;
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        ControlVibrate(); // 4초 후 다음 진동으로 이동
                                    }
                                }, 4000);
                            }}, 1000);
                        break;

                    case MotionEvent.ACTION_UP: // 버튼에서 손 뗌
                        btn.setBackgroundColor(Color.parseColor("#FFD146"));
                        BtnPress = false;
                        break;
                }
                return false;
            }
        });

        btn2 = view.findViewById(R.id.Vibrator_btn2);
        btn2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: // 버튼 누름
                        btn2.setBackgroundColor(Color.parseColor("#D3D3D3")); // 누르는 동안 회색으로 바뀜
                        BtnPress = true;
                        // 1초 동안 누르면 진동 중단
                        handler.postDelayed(() -> {
                            if (BtnPress && isVibrate) { // 1초 동안 누르는 것 성공 && 진동 중
                                correct = true; // 정답
                                StopVibrate(); // 진동 중지
                                BtnPress = false;
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        ControlVibrate(); // 4초 후 다음 진동으로 이동
                                    }
                                }, 4000);
                            }}, 1000);
                        break;

                    case MotionEvent.ACTION_UP: // 버튼에서 손 뗌
                        btn2.setBackgroundColor(Color.parseColor("#FFD146"));
                        BtnPress = false;
                        break;
                }
                return false;
            }
        });

        ReadyVibrate(); //초기화
        Vibrator_Comment.setText("왼쪽 손으로 휴대폰을 잡아주세요 \n 진동이 느껴지면 버튼을 꾹 눌러주세요");

        // 버튼 클릭 소리
        soundPool = new SoundPool.Builder().setMaxStreams(1).build();
        soundId = soundPool.load(getContext(), R.raw.button2, 1);

        // 시작
        if(activity.vib_phase == 0){
            activity.Speak(requireContext(), Vibrator_Comment, "STT", () -> {ControlVibrate();});
        }else {
            activity.Speak(requireContext(), Vibrator_Comment2, "STT", () -> {ControlVibrate();});
        }


        return view;
    }



    private void ReadyVibrate(){
        // 초기화
        BtnPress = false; isVibrate= false;
        round = 0; // 초기 라운드
        score = 0; // 초기 스코어
        random_round = (int) (Math.random() * 3) + 3; // 진동 횟수 : 3~5 사이의 랜덤 숫자 발생
        vib_second = 3000; // 진동 길이
        vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
        //Log.d("VibratorFragment", " round : " + random_round);


        if(activity.vib_phase == 0){
            Vibrator_view1.setVisibility(View.VISIBLE);
            Vibrator_view2.setVisibility(View.GONE);
        } else {
            Vibrator_view1.setVisibility(View.GONE);
            Vibrator_view2.setVisibility(View.VISIBLE);
        }
    }

    private void ControlVibrate(){
        handler.removeCallbacksAndMessages(null); // 이전 작업 제거

        if(round < random_round){ // 아직 정해진 round 만큼 진동하지 않았을 때 다시 진동하도록 control
            round++;StartVibrate();
        } else{ // 정해진 진동을 완료하였을 때 최종 스코어 계산 후 다음 프레그먼트로 이동
            Calculate_Final_Score(); vibrator = null;

            if(activity.vib_phase == 0){
                activity.ChangeFragment("Vibrate"); activity.vib_phase++;
            } else{
                activity.ChangeFragment("EyeDetect");
            }

        }
    }

    Vibrator vibrator;
    int vib_second;
    int[] vib_strengths = {25, 50, 100, 150};

    private void StartVibrate(){
        Log.d("VibratorFragment", "Current Round: " + round + " / " + random_round); vib.setVisibility(View.VISIBLE);

        // strength 랜덤으로 결정
        int strength = vib_strengths[(int) (Math.random() * vib_strengths.length)];

        if (Build.VERSION.SDK_INT >= 26) {
            isVibrate = true;
            vibrator.vibrate(VibrationEffect.createOneShot(vib_second, strength)); // vib_second 동안 strength 세기로 진동
        } else {
            isVibrate = true;
            vibrator.vibrate(vib_second); // vib_second 동안 진동
        }


        // 진동 멈출때 false로 변경
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                StopVibrate();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ControlVibrate(); // 다음 진동으로 이동
                        correct = false;
                    }
                }, 4000);

            }}, vib_second);
    }

    private void StopVibrate(){
        //화면 왼쪽 위 진동중 TEXT용(추후 삭제 요망)
        if(vibrator != null){isVibrate = false; vibrator.cancel(); vib.setVisibility(View.INVISIBLE);
        } else{  }

        // 진동 멈출때 correct가 true 아니라면 스코어 상승
        if(!correct){
            score++; Log.d("VibratorFragment", "score up! : " + score); correct = false;
        }


    }

    private void Calculate_Final_Score(){


        Log.d("VibratorFragment", " final score : " + score);

        if(round == random_round){ // 계산
//            if(score >= random_round){
//                NihssActivity.NIHSS_total_score = NihssActivity.NIHSS_total_score + 2;
//                Toast.makeText(getContext(), "2점 추가. 총 " + NihssActivity.NIHSS_total_score, Toast.LENGTH_SHORT).show();
//            } else if(score <= 1){
//                NihssActivity.NIHSS_total_score = NihssActivity.NIHSS_total_score + 0;
//                Toast.makeText(getContext(), "0점 추가. 총 " + NihssActivity.NIHSS_total_score, Toast.LENGTH_SHORT).show();
//            } else {
//                NihssActivity.NIHSS_total_score = NihssActivity.NIHSS_total_score + 1;
//                Toast.makeText(getContext(), "1점 추가. 총 " + NihssActivity.NIHSS_total_score, Toast.LENGTH_SHORT).show();
//            }

            if(score <= 1){
                NihssActivity.NIHSS_total_score = NihssActivity.NIHSS_total_score + 0;
                Toast.makeText(getContext(), "0점 추가. 총 " + NihssActivity.NIHSS_total_score, Toast.LENGTH_SHORT).show();
            } else {
                NihssActivity.NIHSS_total_score = NihssActivity.NIHSS_total_score + 1;
                Toast.makeText(getContext(), "1점 추가. 총 " + NihssActivity.NIHSS_total_score, Toast.LENGTH_SHORT).show();
            }


        } else{
        }

    }
}


//적절한 진동 길이

//1. 알림/메시지 알림용 진동
//일반적으로 200~300 밀리초 정도의 짧은 진동이 적합합니다.
//너무 짧으면 사용자가 인지하기 어렵고, 너무 길면 불편할 수 있습니다.
//        예: 200~300ms

//2. 중요 알림 (예: 경고 또는 긴급 알림)
//500~1000 밀리초 정도의 진동이 적절합니다.
//중요한 알림일수록 길이를 조금 늘려서 사용자에게 강조할 수 있습니다.
//        예: 500~1000ms

//3. 게임 또는 피드백용 진동
//100 밀리초 이하의 짧은 진동을 여러 번 반복하거나 패턴으로 설정하여 피드백을 줍니다.
//버튼 누름이나 상호작용에 따른 피드백용으로 자주 사용됩니다.
//        예: 100ms씩 여러 번 반복

//4. 긴 알림 패턴
//특정 작업의 완료나 성공 여부 등을 알릴 때는 짧은 진동을 여러 번 반복하는 패턴을 사용할 수 있습니다.
//        예: 100ms 진동 - 100ms 정지 - 100ms 진동 같은 패턴을 설정하면 사용자에게 확실히 인지될 수 있습니다.


//적절한 진동 세기

//일반적인 알림 용도: 50 ~ 100 정도의 값이 적절합니다.
//강한 진동이 필요한 경우: 150 ~ 200 정도가 좋습니다.
//매우 강한 진동이 필요한 경우: 200 ~ 255까지 설정할 수 있지만, 이 경우 배터리 소모와 장치의 진동 모터 성능을 고려해야 합니다.
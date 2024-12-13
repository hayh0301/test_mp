package com.example.nihss_mp.NIHSS;

import android.content.Context;
import android.media.SoundPool;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.CountDownTimer;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nihss_mp.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link EyeDetect2Fragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class EyeDetect2Fragment extends Fragment {

    // 엑티비티와 연결...
    NihssActivity activity;
    @Override public void onAttach(Context context) {super.onAttach(context); if (getActivity() instanceof NihssActivity) {activity = (NihssActivity) getActivity();}}

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public EyeDetect2Fragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment EyeDetect2Fragment.
     */
    // TODO: Rename and change types and number of parameters
    public static EyeDetect2Fragment newInstance(String param1, String param2) {
        EyeDetect2Fragment fragment = new EyeDetect2Fragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    TextView Eyedetect2_Dot_Comment;
    Button eye_btn_1;
    Button eye_btn_2;
    Button eye_btn_3;
    Button eye_btn_4;
    Boolean btn_1_Flag, btn_2_Flag, btn_3_Flag, btn_4_Flag;

    SoundPool soundPool;
    private int soundId;

    Handler handler = new Handler();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_eyedetect2, container, false);

        Eyedetect2_Dot_Comment = view.findViewById(R.id.Eyedetect2_Dot_Comment);
        eye_btn_1 = view.findViewById(R.id.eye_btn_1);
        eye_btn_2 = view.findViewById(R.id.eye_btn_2);
        eye_btn_3 = view.findViewById(R.id.eye_btn_3);
        eye_btn_4 = view.findViewById(R.id.eye_btn_4);

        // 사운드 바꿔야함. 너무 구림
        soundPool = new SoundPool.Builder().setMaxStreams(5).build();
        soundId = soundPool.load(requireContext(), R.raw.button3, 1);

        //초기화
        ReadyEyeDetect2();

        eye_btn_1.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                soundPool.play(soundId, 1, 1, 0, 0, 1); btn_1_Flag = true;}});
        eye_btn_2.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                soundPool.play(soundId, 1, 1, 0, 0, 1); btn_2_Flag = true;}});
        eye_btn_3.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                soundPool.play(soundId, 1, 1, 0, 0, 1); btn_3_Flag = true;}});
        eye_btn_4.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                soundPool.play(soundId, 1, 1, 0, 0, 1); btn_4_Flag = true;}});

        // 텍스트 speak 후 시작
        activity.Speak(requireContext(), Eyedetect2_Dot_Comment, "STT", () -> {
            try {
                Control_Eye_Detect2();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        return view;
    }

    //초기화
    private void ReadyEyeDetect2(){
        btn_1_Flag = false; btn_2_Flag = false;
        btn_3_Flag = false; btn_4_Flag = false;
        round = 0; EyeDetect2_Score = 0;
        // int random_num = (int) (Math.random() * 2) + 1; // 1~3 사이의 랜덤 숫자 발생
        random_num = 8; // 일단은 8번 해보도록하고 구체적인 횟수는 조정 필요
    }

    int round; int random_num; int EyeDetect2_Score;

    private void Control_Eye_Detect2() throws InterruptedException {
        if(round < random_num) { Start_Eye_Detect2(); }
        else if(round == random_num) { // 모든 검사 후 점수 체크
            if(EyeDetect2_Score >= 6){
                Toast.makeText(getContext(),"NIHSS 2점", Toast.LENGTH_SHORT).show();
            } else if (3 <= EyeDetect2_Score && EyeDetect2_Score <6) {
                Toast.makeText(getContext(),"NIHSS 1점", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(),"NIHSS 0점", Toast.LENGTH_SHORT).show();
            }

            if (soundPool != null) {
                soundPool.release(); // 리소스 해제
                soundPool = null;
            }

            activity.ChangeFragment("Draw");


        }
    }

    private void Start_Eye_Detect2() throws InterruptedException {
        btn_1_Flag = false; btn_2_Flag = false;
        btn_3_Flag = false; btn_4_Flag = false;
        int Dot_Control_num = (int) (Math.random() * 4 + 1); // 1~4의 랜덤 숫자
        Dot_Control2(Dot_Control_num);

        handler.removeCallbacksAndMessages(null); // 핸들러에 등록된 모든 작업 취소

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Score(Dot_Control_num);
                if (countDownTimer != null) {
                    countDownTimer.cancel(); // 타이머 종료
                    countDownTimer = null;
                } Dot_Control2(0);
                // 점 비활성화 후 2초 텀 추가
                handler.postDelayed(() -> {
                    try {
                        round++; // 라운드 증가
                        Control_Eye_Detect2(); // 다음 라운드로 이동
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                }, 2 * 1000); // 2초 대기
            }
        }, (long) 3 * 1000); // 3초 후 실행. -> 검사, 다음 라운드 이동

        //Thread.sleep( 6 * 1000);

    }

    private void Dot_Control2(int num){

        switch (num){
            case 0 : // 모두 off
                eye_btn_1.setText("1");
                eye_btn_2.setText("2");
                eye_btn_3.setText("3");
                eye_btn_4.setText("4");
                break;
            case 1 : // 1 깜빡거리기 on 나머지 off
                TicTock(eye_btn_1, 3 * 1000, 500, num);
                eye_btn_2.setText("2");
                eye_btn_3.setText("3");
                eye_btn_4.setText("4");
                break;
            case 2 : // 2 깜빡거리기 on 나머지 off
                eye_btn_1.setText("1");
                TicTock(eye_btn_2, 3 * 1000, 500, num);
                eye_btn_3.setText("3");
                eye_btn_4.setText("4");
                break;
            case 3 : // 3 깜빡거리기 on 나머지 off
                eye_btn_1.setText("1");
                eye_btn_2.setText("2");
                TicTock(eye_btn_3, 3 * 1000, 500, num);
                eye_btn_4.setText("4");
                break;
            case 4 : // 4 깜빡거리기 on 나머지 off
                eye_btn_1.setText("1");
                eye_btn_2.setText("2");
                TicTock(eye_btn_4, 3 * 1000, 500, num);
                eye_btn_4.setText(" ");
                break;
        }

    }


    private void Score(int num){ // 잘못된 버튼을 눌렀을 경우 점수를 +하지는 않도록... 할까 싶음
        switch (num){
            case 0 : // 모두 off
                break;
            case 1 : // 1 깜빡거리기 on 나머지 off
                if(btn_1_Flag == false){EyeDetect2_Score++;} break;
            case 2 : // 2 깜빡거리기 on 나머지 off
                if(btn_2_Flag == false){EyeDetect2_Score++;} break;
            case 3 : // 3 깜빡거리기 on 나머지 off
                if(btn_3_Flag == false){EyeDetect2_Score++;} break;
            case 4 : // 4 깜빡거리기 on 나머지 off
                if(btn_4_Flag == false){EyeDetect2_Score++;} break;
        }
    }

    CountDownTimer countDownTimer;
    private void TicTock(Button button, long totalDuration, long interval, int num) {
        countDownTimer = new CountDownTimer(totalDuration, interval) {
            @Override
            public void onTick(long millisUntilFinished) {
                // 텍스트 토글
                if (button.getText().equals("")) {
                    button.setText(num + "");
                } else {
                    button.setText("");
                }

                // 정답 버튼을 누르면 바로 다음 라운드로 이동
                if (CheckCorrectFlag(num)) {
                    cancel(); // 타이머 종료
                    button.setText(num + ""); // 초기화
                    handler.postDelayed(() -> {
                        try {
                            Dot_Control2(0); // 점 비활성화
                            round++; // 라운드 증가
                            Control_Eye_Detect2(); // 다음 라운드로 이동
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }, 2 * 1000); // 2초 대기 후 다음 라운드로 이동
                }
            }

            @Override
            public void onFinish() {
                button.setText(num + ""); // 반복 종료 후 초기화
            }
        };

        countDownTimer.start(); // 타이머 시작
    }

    private boolean CheckCorrectFlag(int num) {
        switch (num) {
            case 1:
                return btn_1_Flag; // 버튼 1 정답 여부 확인
            case 2:
                return btn_2_Flag; // 버튼 2 정답 여부 확인
            case 3:
                return btn_3_Flag; // 버튼 3 정답 여부 확인
            case 4:
                return btn_4_Flag; // 버튼 4 정답 여부 확인
            default:
                return false;
        }

    }


}
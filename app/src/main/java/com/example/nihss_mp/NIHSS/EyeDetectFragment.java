package com.example.nihss_mp.NIHSS;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nihss_mp.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link EyeDetectFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class EyeDetectFragment extends Fragment {

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

    public EyeDetectFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment EyeDetectFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static EyeDetectFragment newInstance(String param1, String param2) {
        EyeDetectFragment fragment = new EyeDetectFragment();
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

    ImageButton Right_Dot;
    ImageButton Left_Dot;
    TextView Eyedetect_Dot_Comment;
    SoundPool soundPool;
    private int soundId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_eyedetect, container, false);

        Right_Dot = view.findViewById(R.id.Right_Dot);
        Left_Dot = view.findViewById(R.id.Left_Dot);
        Eyedetect_Dot_Comment = view.findViewById(R.id.Eyedetect_Dot_Comment);

        // 사운드
        soundPool = new SoundPool.Builder().setMaxStreams(5).build();
        soundId = soundPool.load(requireContext(), R.raw.button3, 1);

        //초기화
        ReadyEyeDetect();


        Right_Dot.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
            soundPool.play(soundId, 1, 1, 0, 0, 1); Right_flag = true; CheckAndProceed(Dot_Control_num);}});
        Left_Dot.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                soundPool.play(soundId, 1, 1, 0, 0, 1); Left_flag = true; CheckAndProceed(Dot_Control_num);}});

        activity.Speak(requireContext(), Eyedetect_Dot_Comment, "STT", () -> {
            try {
                Control_Eye_Detect();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        return view;
    }

    //초기화
    private void ReadyEyeDetect(){
        Right_Dot.setVisibility(View.GONE);
        Left_Dot.setVisibility(View.GONE);
        Right_flag = false;
        Left_flag = false;
        EyeDetect_Score = 0;
        round = 0;
        // int random_num = (int) (Math.random() * 2) + 1; // 1~3 사이의 랜덤 숫자 발생
        random_num = 8; // 일단은 8번 해보도록하고 구체적인 횟수는 조정 필요
        Dot_Control_num=0;
    }



    Handler handler = new Handler(); // 핸들러 선언
    boolean Right_flag = false;
    boolean Left_flag = false;
    int round;
    int random_num;

    private void Control_Eye_Detect() throws InterruptedException {
        if(round < random_num) { Start_Eye_Detect(); }
        else if(round == random_num) { // 모든 검사 후 점수 체크
            if(EyeDetect_Score >= 6){
                Toast.makeText(getContext(),"NIHSS 2점", Toast.LENGTH_SHORT).show();
            } else if (3 <= EyeDetect_Score && EyeDetect_Score <6) {
                Toast.makeText(getContext(),"NIHSS 1점", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(),"NIHSS 0점", Toast.LENGTH_SHORT).show();
            }

            if (soundPool != null) {
                soundPool.release(); // 리소스 해제
                soundPool = null;
            }

            activity.ChangeFragment("EyeDetect2");


        }

    }

    int Dot_Control_num;

    private void Start_Eye_Detect() throws InterruptedException {
        Right_flag = false; Left_flag = false;
        Dot_Control_num = (int) (Math.random() * 3 + 1); // 1~3의 랜덤 숫자
        Dot_Control(Dot_Control_num);

        handler.removeCallbacksAndMessages(null); // 핸들러에 등록된 모든 작업 취소

        // 핸들러를 3초 내 버튼 클릭 확인을 위해 설정
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // 3초 내에 버튼을 모두 누르지 못했을 경우
                if (!Check_All_Flags(Dot_Control_num)) {
                    Score(Dot_Control_num); // 점수 추가
                }
                Dot_Control(0); // 점 비활성화

                // 2초 텀 추가 후 다음 라운드로 이동
                handler.postDelayed(() -> {
                    try {
                        round++; // 라운드 증가
                        Control_Eye_Detect(); // 다음 라운드로 이동
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }, 2 * 1000); // 2초 대기
            }
        }, (long) 3 * 1000); // 3초 대기

        // 버튼 클릭 이벤트가 3초 내 모두 눌렸을 경우 바로 다음 라운드로 이동
//        if (Check_All_Flags(Dot_Control_num)) { Dot_Control(0); // 점 비활성화
//            handler.post(() -> {
//                    // 2초 텀 추가 후 다음 라운드로 이동
//                    handler.postDelayed(() -> {
//                        try {
//                            round++; // 라운드 증가
//                            Control_Eye_Detect(); // 다음 라운드로 이동
//                        } catch (InterruptedException e) {
//                            throw new RuntimeException(e);
//                        }
//                    }, 2 * 1000); // 2초 대기
//
//            });
//        }

    }





    private void Dot_Control(int num){

        switch (num){
            case 0 : // 모두 off
                Right_Dot.setVisibility(View.GONE);
                Left_Dot.setVisibility(View.GONE); break;
            case 1 : // 오른쪽 on 왼쪽 off
                Right_Dot.setVisibility(View.VISIBLE);
                Left_Dot.setVisibility(View.GONE);break;
            case 2 : // 오른쪽 off 왼쪽 on
                Right_Dot.setVisibility(View.GONE);
                Left_Dot.setVisibility(View.VISIBLE);break;
            case 3 : // 오른쪽 on 왼쪽 on
                Right_Dot.setVisibility(View.VISIBLE);
                Left_Dot.setVisibility(View.VISIBLE);break;
        }

    }

    int EyeDetect_Score;

    private void Score(int num){
        switch (num){
            case 0 : // 모두 off
                break;
            case 1 : // 오른쪽 on 왼쪽 off
                if(Right_flag == false){EyeDetect_Score++;} break;
            case 2 : // 오른쪽 off 왼쪽 on
                if(Left_flag == false){EyeDetect_Score++;} break;
            case 3 : // 오른쪽 on 왼쪽 on
                if(Right_flag == false || Left_flag == false){EyeDetect_Score++;} break;
        }
    }

// 활성화된 버튼이 모두 눌렸는지 확인하는 메서드
    private boolean Check_All_Flags(int num) {
        switch (num) {
            case 1: // 오른쪽 on, 왼쪽 off
                return Right_flag;
            case 2: // 오른쪽 off, 왼쪽 on
                return Left_flag;
            case 3: // 오른쪽 on, 왼쪽 on
                return Right_flag && Left_flag;
            default:
                return false;
        }
    }

    boolean isRoundInProgress = false; // 라운드 진행 상태 플래그

    private void CheckAndProceed(int num) {
        if (isRoundInProgress) return; // 이미 라운드 진행 중이면 중단
        if (Check_All_Flags(num)) { // 모든 버튼이 알맞게 눌렸다면
            isRoundInProgress = true; // 라운드 진행 상태 설정
            Dot_Control(0); // 점 비활성화
            handler.postDelayed(() -> { // 2초 대기 후 다음 라운드로 이동
                try {
                    round++; // 라운드 증가
                    isRoundInProgress = false; // 라운드 진행 상태 해제
                    Control_Eye_Detect(); // 다음 라운드로 이동
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }, 2 * 1000);
        }
    }
}
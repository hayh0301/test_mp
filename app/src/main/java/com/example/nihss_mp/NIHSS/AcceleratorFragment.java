package com.example.nihss_mp.NIHSS;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.nihss_mp.MovingAverage;
import com.example.nihss_mp.R;

public class AcceleratorFragment extends Fragment {

    // 엑티비티와 연결...
    NihssActivity activity;
    @Override public void onAttach(Context context) {super.onAttach(context); if (getActivity() instanceof NihssActivity) {activity = (NihssActivity) getActivity();}}

    private TextView Accelerate_Comment;
    private TextView acc_xyz;
    private TextView accma_xyz;
    private TextView gyro_xyz;
    private TextView gyroma_xyz;

    private Handler handler = new Handler();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_accelerator, container, false);

        // xml과 연결
        Accelerate_Comment = view.findViewById(R.id.Accelerate_Comment);
        acc_xyz = view.findViewById(R.id.acc_xyz);
        accma_xyz = view.findViewById(R.id.accma_xyz);
        gyro_xyz = view.findViewById(R.id.gyro_xyz);
        gyroma_xyz = view.findViewById(R.id.gyroma_xyz);

        // 이동 평균 설정
        xMovingAverage_acc = new MovingAverage(10); // 최근 10개의 데이터만 고려
        yMovingAverage_acc = new MovingAverage(10);
        zMovingAverage_acc = new MovingAverage(10);
        xMovingAverage_gyro = new MovingAverage(10);
        yMovingAverage_gyro = new MovingAverage(10);
        zMovingAverage_gyro = new MovingAverage(10);

        // 센서 준비
        if(activity.acc_phase == 0){
            Accelerate_Comment.setText("왼손에 폰을 올려주세요");
        } else {
            Accelerate_Comment.setText("오른손에 폰을 올려주세요");
        }

        
        ReadySensor();

        // 안내 메세지 전달
        activity.Speak(requireContext(), Accelerate_Comment, "Accelerate", () -> {

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            Accelerate_Comment.setText("이제 어깨 높이까지 폰을 들어올려주세요");
            activity.Speak(requireContext(), Accelerate_Comment, "Accelerate", () -> {});
            Start = true; StartSensor();
        });

        return view;
    }

    private SensorManager sensorManager;
    private SensorEventListener sensorEventListener;
    private Sensor accelerometer;
    private Sensor gyroscope;

    MovingAverage xMovingAverage_acc;
    MovingAverage yMovingAverage_acc;
    MovingAverage zMovingAverage_acc;
    MovingAverage xMovingAverage_gyro;
    MovingAverage yMovingAverage_gyro;
    MovingAverage zMovingAverage_gyro;

    private float acc_initialX; // 초기 X값 (가속도계)
    private float acc_initialZ; // 초기 Z값 (가속도계)
    private float gyro_initialX; // 초기 X값 (자이로스코프)
    private float gyro_initialY; // 초기 Y값 (자이로스코프)
    float xChange; float zChange;


    private void ReadySensor() {
        // 센서 관리자 초기화
        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);

        // 가속도계 및 자이로스코프 초기화
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // 하나의 SensorEventListener로 결합
        sensorEventListener = new SensorEventListener(){
            @Override
            public void onSensorChanged(SensorEvent event) { // 센서 값이 변할 때마다 작동

                // 가속도계일 경우
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    float x = event.values[0]; // X축 가속도 / 가로뷰 휴대폰을 들어올렸을때 이 값이 변함. 휴대폰 방향에 따라 -일지 +일지는 다름. 따라서 절댓값으로 비교하려고함.
//                    float y = event.values[1];
                    float z = event.values[2]; // Z축 가속도 / 휴대폰이 움직였을때 값이 변동됨. 값이 예민한거같음. 이미 9.8 정도의 값을 가지고 있음. 최대 최소 값의 비교를 통해 떨어뜨렸는지 볼까 싶음
//                    acc_xyz.setText(String.format("가속도 x : %.2f, y : %.2f, z : %.2f", x, y, z));
                    acc_xyz.setText(String.format("가속도 x : %.2f, z : %.2f", x, z));

                    xMovingAverage_acc.addData(x); // X축 데이터 이동 평균 추가
//                    yMovingAverage_acc.addData(y); // Y축 데이터 이동 평균 추가
                    zMovingAverage_acc.addData(z); // Z축 데이터 이동 평균 추가
                    float avgX = xMovingAverage_acc.getAverage();
//                    float avgY = zMovingAverage_acc.getAverage();
                    float avgZ = yMovingAverage_acc.getAverage();
//                    accma_xyz.setText(String.format("가속도 이동 평균 x : %.2f, y : %.2f, z : %.2f", avgX, avgY, avgZ));

                    if(acc_initialX == 0.0f){ acc_initialX = x; }
                    if(acc_initialZ == 0.0f){ acc_initialZ = z; }

                    xChange = avgX - acc_initialX;
                    zChange = acc_initialZ - z;
                    //if(z < 9.0) { wasZsmall9 = true; }
                    //if(zChange > 5.0) { wasZsmall9 = true; }

                    accma_xyz.setText(String.format("가속도 이동 평균 x : %.2f, xChange : %.2f", avgX, xChange));

                }
                // 자이로스코프일 경우
                else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                    float x = event.values[0];
                    float y = event.values[1]; // Y축 각속도 / 가속도계의 z값보단 덜 예민한듯. 움직였는지 여부는 얘로 하는게 더 좋아 보임.
//                    float z = event.values[2];
//                    gyro_xyz.setText(String.format("자이로 x : %.2f, y : %.2f, z : %.2f", x, y, z));
                    gyro_xyz.setText(String.format("자이로 x : %.2f, y : %.2f", x, y));

//                    xMovingAverage_gyro.addData(x); // X축 데이터 이동 평균 추가
//                    yMovingAverage_gyro.addData(y); // Y축 데이터 이동 평균 추가
//                    zMovingAverage_gyro.addData(z); // Z축 데이터 이동 평균 추가
//                    float avgX = xMovingAverage_gyro.getAverage();
//                    float avgY = zMovingAverage_gyro.getAverage();
//                    float avgZ = zMovingAverage_gyro.getAverage();
//                    gyroma_xyz.setText(String.format("자이로 이동 평균 x : %.2f, y : %.2f, z : %.2f", avgX, avgY, avgZ));

                    if(gyro_initialX == 0.0f){ gyro_initialX = x; }
                    if(gyro_initialY == 0.0f){ gyro_initialY = y; }

                    if (y >= 0.15) { isHoding = false; } else { isHoding = true; }

                }

                // 이렇게 얻어낸 값들을... 분석하기...
                AnalyzeSensor();
                gyroma_xyz.setText("isHoding : " + isHoding + " / isShoulderHeight : " + isShoulderHeight +
                        "\n / wasLifted : " + wasLifted + " / wasShoulderHeight : " + wasShoulderHeight + " / xChange1 : " + xChange1);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) { // 정확도 처리 관련 문제

            }
        };



    }

    boolean wasShoulderHeight = false; // (한번이라도) 어깨 높이 도달 여부
    boolean wasLifted = false; // (한번이라도) 들어올린 여부
    boolean isShoulderHeight = false;
    boolean isHoding = false; // 가만히 있는지
    boolean Start = false; // 측정 시작 여부
    boolean xChange1 = false; // 측정 시작 여부

    long HoldStartTime = 0; // 가만히 있기 시작한 시간 기록용
    long notShoulderHeightTime = 0; // 어깨 높이가 아닌 시간 기록용

    int i = 1;

    private void AnalyzeSensor(){
        long currentTime = System.currentTimeMillis();

        if(Start){
            // 어깨 높이에 도달했는지 여부 확인
            if(xChange >= 2 ) { isShoulderHeight = true; } else { isShoulderHeight = false; }
            // xChange 구분...
            if(xChange > 0.75) { xChange1 = true;}
            // 한번이라도 움직였는지 여부 확인
            if(isHoding) { if(HoldStartTime == 0) { HoldStartTime = System.currentTimeMillis();  }
            } else { if(HoldStartTime != 0) {wasLifted = true;} HoldStartTime = 0; }

            // 점수 계산...
            // 어깨 높이 도달했을 경우
            if(isShoulderHeight) {
                wasShoulderHeight = true;
                notShoulderHeightTime = 0;
                if(isHoding){

                    if(currentTime - HoldStartTime > 1 * 1000) {
                        if(i == 1){
                            Accelerate_Comment.setText("손을 올린채로 잠시 기다려 주세요.");
                            activity.Speak(getContext(), Accelerate_Comment, "Accelerate", () -> {
                            }); i--;
                        }

                    }
                    if(currentTime - HoldStartTime > 7 * 1000) {
                        StopSensor();
                        Score(0); // 0점 (성공)
                        Accelerate_Comment.setText("측정이 완료되었습니다.");
                        //activity.Speak(getContext(), Accelerate_Comment, "Accelerate", () -> {
                            // 다음 프레그먼트로 이동하는 코드 작성
                            if(activity.acc_phase == 0){
                                activity.ChangeFragment("Accelerator"); activity.acc_phase++;
                            } else{
                                activity.ChangeFragment("FaceDetect");
                            }
                        //});
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } else{
                    //i = 1;
                    HoldStartTime = 0;
                }
            } else {
                if(notShoulderHeightTime == 0) { notShoulderHeightTime = System.currentTimeMillis(); }
                if(currentTime - notShoulderHeightTime > 10 * 1000 ){ // 5초 -> 조절 필요
                    StopSensor();
                    if(wasLifted && wasShoulderHeight){
                        Score(1); // 1점 : 어깨 높이 도달했으나 떨어뜨림
                        Accelerate_Comment.setText("측정이 완료되었습니다.");
                        //activity.Speak(getContext(), Accelerate_Comment, "Accelerate", () -> {
                            // 다음 프레그먼트로 이동하는 코드 작성
                            if(activity.acc_phase == 0){
                                activity.ChangeFragment("Accelerator"); activity.acc_phase++;
                            } else{
                                activity.ChangeFragment("FaceDetect");
                            }
                        //});
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    } else {

                        if (xChange1) {
                            Score(2); // 2점 : 움직이긴 했음
                            Accelerate_Comment.setText("측정이 완료되었습니다.");
                            //activity.Speak(getContext(), Accelerate_Comment, "Accelerate", () -> {
                                // 다음 프레그먼트로 이동하는 코드 작성
                                if(activity.acc_phase == 0){
                                    activity.ChangeFragment("Accelerator"); activity.acc_phase++;
                                } else{
                                    activity.ChangeFragment("FaceDetect");
                                }
                            //});
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            Score(3); // 3점 : 움직이지 않았음
                            Accelerate_Comment.setText("측정이 완료되었습니다.");
                           // activity.Speak(getContext(), Accelerate_Comment, "Accelerate", () -> {
                                // 다음 프레그먼트로 이동하는 코드 작성
                                if(activity.acc_phase == 0){
                                    activity.ChangeFragment("Accelerator"); activity.acc_phase++;
                                } else{
                                    activity.ChangeFragment("FaceDetect");
                                }
                           // });
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }


                }
            }
        }



    }

    private void StartSensor() {
        // 센서 리스너 등록
        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(sensorEventListener, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void StopSensor() {
        Start = false;
        // 센서 리스너 해제
        sensorManager.unregisterListener(sensorEventListener);
    }

    // 점수 화면에 표시하는 함수 추가
    private void Score(int score) {
        String message;
        switch (score) {
            case 0:
                message = "0점: 어깨 높이 유지 성공!";
                break;
            case 1:
                message = "1점: 어깨 높이 유지 실패!";
                break;
            case 2:
                message = "2점: 어깨 높이 도달하지 못함!";
                break;
            case 3:
                message = "3점: 거의 들지 못함!";
                break;
            default:
                message = "알 수 없는 상태!";
                break;
        }
        //Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
        NihssActivity.NIHSS_total_score = NihssActivity.NIHSS_total_score + score;
        Toast.makeText(getContext(), score + "점 추가. 총 " + NihssActivity.NIHSS_total_score, Toast.LENGTH_SHORT).show();
        Log.d("AcceleratorFragment", message);
    }


}








//안드로이드의 가속도 센서(accelerometer)는 스마트폰의 이동과 기울기를 감지하기 위해 사용됩니다. 이 센서는 X, Y, Z 축의 가속도 값을 제공하며, 이 값들은 중력과 함께 특정 방향으로의 가속도를 나타냅니다.

//가속도 센서에서 측정되는 X, Y, Z 값은 다음과 같습니다:

//1. X 축
//왼쪽에서 오른쪽 방향의 가속도를 나타냅니다.
//가로 방향(좌우)으로 기기를 기울이거나 움직일 때 값이 변합니다.
//평평하게 둔 상태에서 오른쪽으로 기울이면 + 값이, 왼쪽으로 기울이면 - 값이 나타납니다.

//2. Y 축
//위에서 아래 방향의 가속도를 나타냅니다.
//세로 방향(앞뒤)으로 기기를 기울이거나 움직일 때 값이 변합니다.
//평평하게 둔 상태에서 위쪽으로 기울이면 + 값이, 아래쪽으로 기울이면 - 값이 나타납니다.

//3. Z 축
//위에서 아래 방향(기기 표면에 수직 방향)의 가속도를 나타냅니다.
//기기를 위아래로 들어 올리거나 내려놓는 동작 시 값이 변합니다.
//중력의 영향을 받기 때문에, 기기가 평평하게 놓여 있으면 약 +9.8 m/s²에 가까운 값이 나옵니다.

//일반적인 XYZ 값의 단위
//각 축의 값은 m/s² (미터/초²) 단위로 표시되며, 중력(g)으로 인해 Z 축 값이 기기가 평평하게 놓여 있을 때 대략 9.8 m/s²로 표시됩니다.
//예를 들어, 기기를 완전히 평평하게 놓으면: X와 Y 값은 약 0에 가까워집니다. Z 값은 약 9.8 m/s²가 됩니다.



//Y 값은 기기를 세로 방향(앞뒤)으로 움직이거나 기울일 때 변합니다. 일반적인 속도로 움직일 때의 Y 값은 상황에 따라 다르지만, 대체로 -9.8 m/s²에서 +9.8 m/s² 사이에서 변동합니다.
//
//Y 값 범위 예시
//정지 상태:
//
//기기가 평평하게 놓여 있을 때 Y 값은 보통 0 m/s²에 가깝습니다.
//기기를 세로로 세워 세우면 중력의 영향을 받아 Y 값이 9.8 m/s² 혹은 -9.8 m/s² 근처로 이동할 수 있습니다.
//일반적인 속도로 앞뒤로 흔드는 경우:
//
//천천히 흔들면 Y 값은 3~7 m/s² 정도의 범위에서 변동합니다.
//평평한 상태에서 기기를 위쪽으로 살짝 기울이거나 아래쪽으로 기울이면 약간의 플러스 혹은 마이너스 값을 보일 수 있습니다.
//빠르게 앞뒤로 흔드는 경우:
//
//빠르게 움직이거나 흔들면 Y 값이 10 m/s² 이상으로 증가하거나 -10 m/s² 이하로 감소할 수 있습니다.
//수직 방향의 움직임:
//
//기기를 위쪽 또는 아래쪽으로 강하게 움직일 때, Y 값이 일시적으로 **중력 가속도(±9.8 m/s²)**에 가까워지며, 더 큰 변화를 나타낼 수 있습니다.
//Y 값 활용
//Y 값은 기기의 기울기와 전후 움직임을 감지하는 데 사용되며, 걷기 동작을 감지하거나, 방향을 측정하는 데 유용합니다.
//Z 값과 마찬가지로 Y 값 역시 기기의 상태와 환경에 따라 달라질 수 있으므로, 대략적인 범위로만 참고하시면 됩니다.





//일반적인 속도로 움직일 때의 Z 값은 기기의 방향, 움직임의 정도, 중력에 의한 영향을 포함하기 때문에 상황에 따라 다르지만, 대개는 9.8 m/s² ± 2~3 정도의 범위에서 변동됩니다.
//
//구체적인 Z 값 범위
//정지 상태: 기기가 평평하게 놓여 있는 경우 Z 값은 대략 9.8 m/s²에 가깝습니다. 이 값은 지구의 중력 가속도를 반영한 기본 값입니다.
//천천히 들어 올리는 경우: 기기를 천천히 위로 들어 올릴 때 Z 값은 약간 증가하거나 감소하여 8~11 m/s² 사이에서 변동될 수 있습니다.
//보통의 속도로 움직이는 경우: 일반적인 속도(손에 쥐고 걸을 때와 같은 속도)로 위아래로 흔들거나 들었다 놓으면 Z 값은 7~13 m/s² 정도로 더 넓게 변할 수 있습니다.
//빠르게 움직이는 경우: 빠르게 위아래로 진동을 주거나 흔들면 Z 값이 15 m/s² 이상까지 크게 증가하거나 0에 가까워질 수 있습니다.
//측정 시 고려사항
//Z 값은 중력(9.8 m/s²)을 포함하므로, 기기가 위아래로 움직일 때마다 중력과 가속도가 합쳐지면서 변동됩니다.
//단순한 움직임을 넘어서 충격을 주거나 빠르게 진동하면 Z 값이 훨씬 더 크게 변할 수 있습니다.
//이 수치는 기기나 측정 환경에 따라 약간의 차이가 있을 수 있으니, 일반적인 기준으로 참고하시면 됩니다.




//자이로스코프(Gyroscope)는 물체의 **각속도(Angular Velocity)**를 측정하는 센서입니다.
// 안드로이드 스마트폰의 자이로스코프는 **기기의 X, Y, Z 축에 대해 초당 회전각(라디안/초)**을 측정하여 각도 변화를 감지할 수 있습니다.
//
//1. X, Y, Z 축 정의
//자이로스코프 값은 기기의 물리적 방향을 기준으로 측정되며, 축은 다음과 같이 정의됩니다:

//X축 (Pitch):
//
//기기의 좌우 회전을 측정.
//기기가 세로로 서 있는 상태에서 앞뒤로 회전(고개를 끄덕이는 동작).
//값이 양수: 기기의 윗부분이 앞으로 기울어짐.
//값이 음수: 기기의 윗부분이 뒤로 기울어짐.

//Y축 (Roll):
//
//기기의 위아래 회전을 측정.
//기기가 세로로 서 있는 상태에서 좌우로 회전(고개를 좌우로 기울이는 동작).
//값이 양수: 기기의 오른쪽이 위로 올라감.
//값이 음수: 기기의 왼쪽이 위로 올라감.

//Z축 (Yaw):
//
//기기의 회전축(위에서 바라본 수직축)을 기준으로 좌우로 회전(고개를 좌우로 돌리는 동작).
//값이 양수: 시계 방향 회전.
//값이 음수: 반시계 방향 회전.


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// 자이로스코프의 y값이 0.1 이상 변했으면 움직인 것. hold 여부는 이걸로 판단하고
// 가속도계의 X 값이 들어올렸을 경우 변하는 듯. 어느 방향으로 들어올릴지 모르겠으므로 절댓값으로 판별하고, 약 2 이상의 값이면 어깨 높이라고 보는게 맞을듯(가끔 4도 나오는데 그건 몰겟음)
// 가속도계의 z 값은 그냥 초기값이랑 비교하는 편이 더 나을듯. 떨어뜨리면 가속도계의 Z 값이 한 11 정도 나오는 듯 함
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

//수정할것들
//문제상황 1. 폰을 기울였을때 0점이 된다 -> 0점 기준에 가속도가 있었는지 판별하는것이 좋을듯
//문제상황 2. 폰을 어디서부터 어디까지 들어야 할지 모르겠음 -> 그림 애니메이션을 넣어서 팔 위치를 처음에 아래로 내리라고 알려주고, (이때 x값을 이용해 특정 값이 되면.. -2 혹은 2로 생각하고 있음) 측정을 시작합니다~
//말한 후 들어올리도록하자. 즉 기준점을 먼저 잡고 시작하도록 하자
//문제상황 3. 2점 나오기가 생각보다 어려운듯, 이것은 최대 x값이 2밖에 되지 않는 문제인데 문제상황 2를 해결하면 어느정도 답이 나올듯하다.

//문제상황과 수정 아이디어
//문제상황 1: 폰을 기울였을 때 0점이 되는 문제
//문제 요약: 가속도가 변화했음에도 불구하고, 단순히 폰을 기울였을 때 0점으로 판별되는 경우 발생.
//수정 아이디어:
//가속도 변화 유무 판별 추가:
//초기값 대비 Z축 변화량(zChange)이 일정 수준 이상이어야만 0점으로 판별.
//예: zChange > 0.5f일 경우에만 어깨 높이에 도달한 것으로 간주.
//자이로스코프 값 확인:
//Z축 가속도가 유지되더라도 자이로스코프 값(y)의 안정성이 중요한 판단 요소.
//기울임 상태에서 자이로스코프의 Y축 변화량(Math.abs(y - gyro_initialY))이 높으면 기울임으로 판별하고 0점에서 제외.
//문제상황 2: 어디서부터 어디까지 들어야 할지 모르는 문제
//문제 요약: 사용자가 폰을 얼마나 들어올려야 하는지에 대한 기준이 명확하지 않아 혼란 발생.
//수정 아이디어:
//그림 또는 애니메이션 추가:
//화면에 팔의 초기 위치와 목표 위치를 시각적으로 보여줌.
//간단한 애니메이션(예: 팔이 아래에서 위로 올라가는 이미지)을 추가해 시작점을 명확히 알림.
//초기 위치 기준 설정:
//시작 전에 사용자가 팔을 아래로 내리도록 유도.
//X축 가속도 초기값을 일정 기준 값으로 설정(예: -2 또는 2).
//X값이 기준치에 도달하면 "측정을 시작합니다~"라는 음성 안내 후 가속도와 자이로스코프 값 측정을 시작.
//단계별 안내:
//        "폰을 아래로 내리세요" → X값이 기준에 도달 → "측정을 시작합니다. 폰을 들어올려주세요."
//문제상황 3: 2점 판별이 어려운 문제
//문제 요약: 최대 X값이 2로 제한되며, 어깨 높이에 미달한 경우를 판별하기 어려움.
//수정 아이디어:
//문제상황 2 해결로 개선 가능:
//초기 X값 기준을 명확히 설정하면, 어깨 높이에 미달한 경우의 판단 기준(X값 증가량)을 보다 정밀히 측정 가능.
//X축 변화량 기준 하향 조정:
//최대 X값 기준이 2밖에 안 되는 상황에서, 어깨 높이에 도달하지 못한 경우를 더 민감하게 감지하도록 조건 완화.
//        예: zChange > 1.5f와 xChange > 1.0f를 동시에 확인.
//최대 X값과 Z값 조합:
//Z축 가속도가 충분히 상승했으나, X축 변화량이 기준 미달인 경우를 2점으로 판별.
//예: "Z축 최대값은 2.0 이상, X축 변화량은 1.5 미만."
//추가 아이디어
//높이 기준 추가:
//
//Z값 변화량 + 초기값 기준 설정:
//예: initialZ + 2.0f 이상의 값이 유지될 때 어깨 높이 도달로 판별.
//시작점을 확실히 잡으면 높이 기준 설정이 쉬워짐.
//움직임 패턴 분석:
//
//폰을 들어올리는 패턴(가속도 + 자이로스코프 변화)을 기반으로, 어깨 높이에 미달한 경우와 단순 움직임을 구분.
//리드백 제공:
//
//        "폰을 더 들어올려 주세요!"와 같은 리드백 메시지를 추가해 사용자가 목표 높이에 도달하도록 유도.
//유지 시간 측정:
//
//사용자가 목표 높이에 도달하지 못했더라도 일정 시간 동안 유지한 경우를 별도로 판단(예: 2.5초 이상 유지 시 1.5점과 같은 중간 점수).
//최종 로직 정리
//Step 1: 폰을 아래로 내리도록 유도 → 초기 X값 및 Z값 기록.
//        Step 2: 폰을 들어올릴 때의 X값 변화량 및 Z축 최대값 확인.
//        Step 3: 어깨 높이에 도달 시 유지 시간 측정(5초).
//Step 4: 어깨 높이에 도달하지 못했지만 움직임 감지 시 2점.
//        Step 5: 모든 변화가 없는 경우 3점.
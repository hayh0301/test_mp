package com.example.nihss_mp.NIHSS;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nihss_mp.MovingAverage;
import com.example.nihss_mp.R;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class FaceDetectFragment extends Fragment {

    // 액티비티와 연결
    NihssActivity activity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getActivity() instanceof NihssActivity) {
            activity = (NihssActivity) getActivity();
        }
    }

    // 뷰 요소 정의
    TextView FaceDetect_Comment; // 안내 메시지 출력
    TextView data1; // 분석 결과 출력
    TextView data2;
    PreviewView FaceDetect_Preview; // 카메라 프리뷰
    ImageView LandmarkOverlay; // 랜드마크 오버레이
    Handler handler = new Handler(); // 작업 스케줄러

    // Mediapipe FaceLandmarker 객체
    FaceLandmarker faceLandmarker;

    // 분석 관련 상태 변수
    boolean initial = false;
    boolean Face = false;
    boolean CornerMove = false;

    // 초기값 및 거리 데이터
    float initial_MouthDistance_Left = 0;
    float initial_MouthDistance_Right = 0;
    float initial_Left_x = 0;
    float initial_Right_x = 0;
    float initial_Left_y = 0;
    float initial_Right_y = 0;
    float ydiff = 0;

    // 이미지 크기
    int wid = 0;
    int hei = 0;

    // 분석 결과 점수
    int score;

    long HoldStartTime = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_facedetect, container, false);

        // 레이아웃 요소 연결
        FaceDetect_Comment = view.findViewById(R.id.FaceDetect_Comment);
        FaceDetect_Preview = view.findViewById(R.id.FaceDetect_Preview);
        LandmarkOverlay = view.findViewById(R.id.LandmarkOverlay);
        data1 = view.findViewById(R.id.data1);
        data2 = view.findViewById(R.id.data2);

        // 카메라 및 얼굴 랜드마커 초기화
        initializeCameraX();
        initializeFaceLandmarker();

        // 안내 메시지 설정 및 음성 출력
        FaceDetect_Comment.setText("얼굴이 잘 보이도록 화면을 조절해주세요");
        activity.Speak(requireContext(), FaceDetect_Comment, "FaceDetect", () -> {

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            long currentTime = System.currentTimeMillis();
            if (Face && (currentTime - HoldStartTime > 1000)) {
                initial = true;
                FaceDetect_Comment.setText("이~ 하고 웃어주세요");
                activity.Speak(requireContext(), FaceDetect_Comment, "FaceDetect", null);
                handler.postDelayed(this::Score, 10000); // 10초 후 점수 계산
            }
        });
        activity.Progressbar_NIHSS.setVisibility(View.GONE);

        return view;
    }

    // 카메라 초기화
    private void initializeCameraX() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // 프리뷰 설정
                Preview preview = new Preview.Builder().build();
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT) // 전면 카메라
                        .build();
                preview.setSurfaceProvider(FaceDetect_Preview.getSurfaceProvider());

                // 이미지 분석 설정
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(requireContext()), this::analyzeImage);

                // 카메라와 분석기 연결
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    // Mediapipe 얼굴 랜드마커 초기화
    private void initializeFaceLandmarker() {
        try {
            // 랜드마커 초기화
            BaseOptions baseOptions = BaseOptions.builder()
                    .setModelAssetPath("face_landmarker.task") // 모델 파일 경로
                    .build();

            FaceLandmarker.FaceLandmarkerOptions options = FaceLandmarker.FaceLandmarkerOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setRunningMode(RunningMode.LIVE_STREAM)
                    .setResultListener(this::analyzeFaceExpression)
                    .build();

            faceLandmarker = FaceLandmarker.createFromOptions(requireContext(), options);
        } catch (Exception e) {
            Log.e("FaceLandmarker", "Error initializing FaceLandmarker: " + e.getMessage());
        }
    }

    // 이미지 분석
    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        if (faceLandmarker != null && imageProxy.getImage() != null) {
            Bitmap bitmap = imageProxyToBitmap(imageProxy);
            wid = imageProxy.getWidth();
            hei = imageProxy.getHeight();

            if (bitmap == null) {
                imageProxy.close();
                return;
            }

            MPImage mpImage = new BitmapImageBuilder(bitmap).build();
            long timestampMs = System.currentTimeMillis();

            faceLandmarker.detectAsync(mpImage, timestampMs);
            imageProxy.close();
        } else {
            imageProxy.close();
        }
    }

    // 이미지 변환
    @OptIn(markerClass = ExperimentalGetImage.class)
    private Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        Image image = imageProxy.getImage();
        if (image == null) return null;

        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new android.graphics.Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, out);
        return BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size());
    }

    // 얼굴 표정 분석
    private void analyzeFaceExpression(FaceLandmarkerResult result, MPImage mpImage) {
        if (result == null || result.faceLandmarks().isEmpty()) return;

        // 첫 번째 얼굴의 랜드마크 가져오기
        List<NormalizedLandmark> landmarks = result.faceLandmarks().get(0);

        // 기준 랜드마크 (입술 및 입꼬리)
        NormalizedLandmark UpMouth = landmarks.get(0); // 위쪽 입술
        NormalizedLandmark DownMouth = landmarks.get(17); // 아래쪽 입술
        NormalizedLandmark leftMouthCorner = landmarks.get(61); // 왼쪽 입꼬리
        NormalizedLandmark rightMouthCorner = landmarks.get(291); // 오른쪽 입꼬리

        // 입 정중앙 계산
        float MouthCenterX = (UpMouth.x() + DownMouth.x()) / 2;
        float MouthCenterY = (UpMouth.y() + DownMouth.y()) / 2;

        // 입꼬리와 정중앙 거리 계산
        float MouthDistance_Left = (float) Math.sqrt(Math.pow(leftMouthCorner.x() - MouthCenterX, 2) +
                Math.pow(leftMouthCorner.y() - MouthCenterY, 2));
        float MouthDistance_Right = (float) Math.sqrt(Math.pow(rightMouthCorner.x() - MouthCenterX, 2) +
                Math.pow(rightMouthCorner.y() - MouthCenterY, 2));

        // 초기값 저장
        if (initial) {
            if (initial_MouthDistance_Left == 0) initial_MouthDistance_Left = MouthDistance_Left;
            if (initial_MouthDistance_Right == 0) initial_MouthDistance_Right = MouthDistance_Right;
            if (initial_Left_x == 0) initial_Left_x = leftMouthCorner.x();
            if (initial_Right_x == 0) initial_Right_x = rightMouthCorner.x();
            if (initial_Left_y == 0) initial_Left_y = leftMouthCorner.y();
            if (initial_Right_y == 0) initial_Right_y = rightMouthCorner.y();
        }

        // 변화율 계산
        float mouthChangeLeftX = ((MouthDistance_Left - initial_MouthDistance_Left) / initial_MouthDistance_Left) * 100;
        float mouthChangeRightX = ((MouthDistance_Right - initial_MouthDistance_Right) / initial_MouthDistance_Right) * 100;

        // 인식된 값에 따라 조절

        // 얼굴이 다 보이는지
        if(leftMouthCorner== null || rightMouthCorner == null ) {Face = false;
        } else {  Face = true; }

        // 얼굴 없었다가 다시 있을때 다시 측정시작
        long StartTime = 0;
        long currentTime = System.currentTimeMillis();
        if(!Face){ initial = false; StartTime = 0; handler.removeCallbacks(null);
            initial_MouthDistance_Left = 0; initial_MouthDistance_Right = 0; initial_Left_x = 0; initial_Right_x=0; initial_Left_y = 0; initial_Right_y=0;ydiff = 0;
        } else {
            if(StartTime == 0){StartTime = currentTime;}
            if(currentTime - StartTime > 1000) {
                initial = true;
                handler.postDelayed(this::Score, 10000);}
        }

        // 양 입꼬리 y좌표 비교
        if(ydiff == 0){ydiff = Math.abs(Math.abs(leftMouthCorner.y()) - Math.abs(rightMouthCorner.y()));}

        if(Math.abs(
                Math.abs(initial_Left_y - leftMouthCorner.y())
                        - Math.abs(initial_Right_y - rightMouthCorner.y())
        ) <0.0065 ){ CornerMove = false; } else {CornerMove=true;}


        // 입꼬리 변화 비율 및 점수 계산
        // 문제점 -> 얼굴 위치, 거리, 기울기 에 따라 값이 달라짐. 수정이 필요함.
        if (Math.abs(mouthChangeLeftX - mouthChangeRightX) < 5 && CornerMove) {
            score = 0; // 정상
        } else if (Math.abs(mouthChangeLeftX - mouthChangeRightX) <= 8 && CornerMove) {
            score = 1; // 경미한 마비
        } else if (Math.abs(mouthChangeLeftX - mouthChangeRightX) > 2) {
            score = 2; // 부분적 마비
        } else {
            score = 3; // 완전 마비
        }

        // 점수 및 상태 로그 출력
        Log.d("FaceDetect", "score : " + score +
                " / mouthChangeLeftX : " + mouthChangeLeftX +
                " / mouthChangeRightX : " + mouthChangeRightX +
                " / CornerMove : " + CornerMove +(Math.abs(
                Math.abs(initial_Left_y - leftMouthCorner.y())
                        - Math.abs(initial_Right_y - rightMouthCorner.y())
        )
                ));

        // 랜드마크 시각화
        visualizeLandmarksWithFlip(landmarks);
    }

    // 랜드마크 시각화
    private void visualizeLandmarksWithFlip(List<NormalizedLandmark> landmarks) {
        Bitmap bitmap = Bitmap.createBitmap(wid, hei, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(2);

        Paint paint2 = new Paint();
        paint2.setColor(Color.BLUE);
        paint2.setStyle(Paint.Style.FILL);
        paint2.setStrokeWidth(2);

        for (NormalizedLandmark landmark : landmarks) {
            // 좌우 반전
            float x = (1.0f - landmark.x()) * bitmap.getWidth();
            float y = landmark.y() * bitmap.getHeight();
            canvas.drawCircle(x, y, 3, paint);
        }

        // 입꼬리 랜드마크 강조
        canvas.drawCircle((1.0f - landmarks.get(61).x()) * bitmap.getWidth(), landmarks.get(61).y() * bitmap.getHeight(), 3, paint2);
        canvas.drawCircle((1.0f - landmarks.get(291).x()) * bitmap.getWidth(), landmarks.get(291).y() * bitmap.getHeight(), 3, paint2);

        requireActivity().runOnUiThread(() -> LandmarkOverlay.setImageBitmap(bitmap));
    }

    // 점수 계산
    private void Score() {
        String message = "";
        switch (score) {
            case 0:
                message = "0점: 정상. 대칭적 운동";
                break;
            case 1:
                message = "1점: 경미한 마비. 웃을 때 비대칭성";
                break;
            case 2:
                message = "2점: 부분적 마비. 얼굴 하부의 완전 마비";
                break;
            case 3:
                message = "3점: 완전 마비. 얼굴 상부 및 하부 움직임 없음";
                break;
        }
        Log.d("FaceDetect", message);

        NihssActivity.NIHSS_total_score = NihssActivity.NIHSS_total_score + score;
        Toast.makeText(getContext(), score + "점 추가. 총 " + NihssActivity.NIHSS_total_score, Toast.LENGTH_SHORT).show();

        data1.setText(message);

        // 카메라 리소스 해제
        try {
            ProcessCameraProvider cameraProvider = ProcessCameraProvider.getInstance(requireContext()).get();
            cameraProvider.unbindAll(); // 모든 카메라 연결 해제
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        // Mediapipe FaceLandmarker 리소스 해제
        if (faceLandmarker != null) {
            faceLandmarker.close(); // Mediapipe 리소스 해제
            faceLandmarker = null; // 참조 해제
        }

        // 기타 핸들러 콜백 제거
        handler.removeCallbacksAndMessages(null);

        activity.ChangeFragment("End");
    }
}




////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//입꼬리가 아닌 다른 부분도 더 비교하는 것으로 바꿔야함.
//생각한 부위
//        1. 눈의 상하 높이
//        2. 볼의 부푼 정도
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//x축: 수평 방향 (왼쪽-오른쪽)
//
//얼굴의 왼쪽은 x값이 작고, 오른쪽은 x값이 커집니다.
//y축: 수직 방향 (위-아래)
//
//얼굴의 위쪽은 y값이 작고, 아래쪽은 y값이 커집니다.
//이 축은 세로 방향을 기준으로 하기 때문에 y축이 위아래를 의미합니다.
//z축: 깊이 방향 (앞-뒤)
//
//얼굴의 앞쪽은 z값이 커지고, 뒤쪽은 z값이 작습니다.
//예시:
//왼쪽 눈: landmarks.get(33) (x, y, z) 좌표를 사용할 때,
//x: 왼쪽-오른쪽 위치
//y: 위-아래 위치 (눈의 수직 위치)
//z: 앞-뒤 깊이
//따라서 미소 짓기나 비대칭적인 얼굴 표정을 비교할 때, 주로 x (좌우), y (위아래), z (깊이) 값을 모두 고려하여 얼굴의 변화를 파악할 수 있습니다.
//
//결론:
//y축은 위-아래를 나타내며, 이를 사용하여 얼굴 표정의 상하 변화나 미소의 차이를 비교할 수 있습니다.



// 양쪽 입술의 입꼬리가 입술 중앙으로부터의 x,y,z 좌표상 떨어져있는 거리가 얼마나 비슷한지 (초기값과 비교했을때 떨어진 거리가 거의 차이가 없다면 움직이지 않은 것이므로 마비라 판별한다, 왼쪽이 마비인지 오른쪽이 마비인지 아니면 전혀 움직이지 않는지도 판별)
// 눈썹 위치가 초기값과 비교해 x,y,z 상 상승했는지... 확인.
// 이 두개 다 비교해서 상부만 마비인지 하부만 마비인지 반쪽만 마비인지 마비없는지


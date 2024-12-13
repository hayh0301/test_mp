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

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.nihss_mp.R;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult;

import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class FaceDetect2Fragment extends Fragment {


    // 액티비티와 연결
    NihssActivity activity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getActivity() instanceof NihssActivity) {
            activity = (NihssActivity) getActivity();
        }
    }

    ImageView LandmarkOverlay; // 랜드마크 오버레이
    TextView FaceDetect2_Comment;
    PreviewView FaceDetect2_Preview;
    TextView data1; // 분석 결과 출력
    TextView data2;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_facedetect2, container, false);

        FaceDetect2_Comment = view.findViewById(R.id.FaceDetect2_Comment);
        FaceDetect2_Preview = view.findViewById(R.id.FaceDetect2_Preview);
        data1 = view.findViewById(R.id.data1);
        data2 = view.findViewById(R.id.data2);
        LandmarkOverlay = view.findViewById(R.id.LandmarkOverlay);

        // 카메라 및 얼굴 랜드마커 초기화
        initializeCameraX();
        initializeFaceLandmarker();


        activity.Speak(requireContext(), FaceDetect2_Comment, "FaceDetect", () -> {});

        return view;
    }

    // 이미지 크기
    int wid = 0;
    int hei = 0;
    // Mediapipe FaceLandmarker 객체
    FaceLandmarker faceLandmarker;
    Handler handler = new Handler(); // 작업 스케줄러
    // 분석 결과 점수
    int score;


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
                preview.setSurfaceProvider(FaceDetect2_Preview.getSurfaceProvider());

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

        // 기준 랜드마크
        NormalizedLandmark LeftEye = landmarks.get(468); // 오른쪽 눈
        NormalizedLandmark RightEye = landmarks.get(473); // 왼쪽 눈


        // 눈 위치에 따른 판별 코드 작성 필요


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

        // 눈 랜드마크 강조
        canvas.drawCircle((1.0f - landmarks.get(468).x()) * bitmap.getWidth(), landmarks.get(448).y() * bitmap.getHeight(), 3, paint2);
        canvas.drawCircle((1.0f - landmarks.get(473).x()) * bitmap.getWidth(), landmarks.get(473).y() * bitmap.getHeight(), 3, paint2);

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

        activity.ChangeFragment("FaceDetect2");
    }

}
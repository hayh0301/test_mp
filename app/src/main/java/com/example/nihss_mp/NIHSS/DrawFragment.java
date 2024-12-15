package com.example.nihss_mp.NIHSS;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.nihss_mp.DrawingView;
import com.example.nihss_mp.R;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;

public class DrawFragment extends Fragment {

    // 엑티비티와 연결...
    NihssActivity activity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        System.loadLibrary("opencv_java4");
        if (getActivity() instanceof NihssActivity) {
            activity = (NihssActivity) getActivity();
        }
    }

    DrawingView Draw_MyImage;
    ImageView Draw_OriImage;
    ImageView Draw_dot;
    TextView Draw_Comment;

    Handler handler = new Handler();
    Bitmap oriBitmap;
    Bitmap userBitmap;
    Mat oriMat;
    Mat userMat;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_draw, container, false);

        Draw_MyImage = view.findViewById(R.id.Draw_MyImage);
        Draw_OriImage = view.findViewById(R.id.Draw_OriImage);
        Draw_dot = view.findViewById(R.id.Draw_dot);
        Draw_Comment = view.findViewById(R.id.Draw_Comment);

        Draw_MyImage.stopDrawing();

        view.post (() -> {
            Log.d("DrawFragment", "my : w - " + Draw_MyImage.getWidth() + " h - " + Draw_MyImage.getHeight());
            Log.d("DrawFragment", "ori : w - " + Draw_OriImage.getWidth() + " h - " + Draw_OriImage.getHeight());

            // 크기 조정 작업

            CanvasSizeControl();
        });

        if(activity.draw_phase !=0){
            Draw_Comment.setText("벽면에 닿지 않게 오른손으로 빨간점을 연결해주세요");
        } else{
            Draw_Comment.setText("벽면에 닿지 않게 왼손으로 빨간점을 연결해주세요");
        }
        activity.Speak(getContext(), Draw_Comment, "Draw", () -> {});


        activity.SetProgressbar(10);
        // 10초 후 비교 작업
        handler.postDelayed(() -> {
            if (Draw_MyImage.isDrawingEnabled) {
                Draw_MyImage.stopDrawing();
            }

            Draw_MyImage.saveCanvasBitmapDebug();

            boolean isCollision = checkCollision();
            if (isCollision) {
                NihssActivity.NIHSS_total_score = NihssActivity.NIHSS_total_score + 1;
                Toast.makeText(getContext(), "1점 추가. 총 " + NihssActivity.NIHSS_total_score, Toast.LENGTH_SHORT).show();
            } else {
                NihssActivity.NIHSS_total_score = NihssActivity.NIHSS_total_score + 0;
                Toast.makeText(getContext(), "0점 추가. 총 " + NihssActivity.NIHSS_total_score, Toast.LENGTH_SHORT).show();
            }

            if(activity.draw_phase == 0){
                activity.ChangeFragment("Draw"); activity.draw_phase++;
            } else{
                activity.ChangeFragment("Accelerator");
            }


        }, 10 * 1000);

        return view;
    }


    private void CanvasSizeControl(){
        if(activity.draw_phase != 0){
            Draw_OriImage.setScaleX(-1);
            Draw_dot.setScaleX(-1);
            Draw_MyImage.setScaleX(-1);
        }

        Bitmap oriBitmapOriginal = ((BitmapDrawable) Draw_OriImage.getDrawable()).getBitmap();
        if (oriBitmapOriginal == null) {
            Log.e("DrawFragment", "원본 비트맵을 가져오지 못했습니다!");
            return;
        }

        // 화면 크기 가져오기
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;

        // 여백 설정 (예: 화면의 10%를 margin으로 사용)
        int margin = (int) (screenWidth * 0.025); // 5% margin
        int availableWidth = screenWidth - (2 * margin);
        int availableHeight = screenHeight - (2 * margin);

        // 원본 비율 계산
        float aspectRatio = (float) oriBitmapOriginal.getWidth() / oriBitmapOriginal.getHeight();

        // 화면 크기에 맞춘 크기 계산
        int targetWidth, targetHeight;
        if (aspectRatio > 1) { // 가로가 긴 이미지
            targetWidth = Math.min(availableWidth, availableHeight);
            targetHeight = (int) (targetWidth / aspectRatio);
        } else { // 세로가 긴 이미지
            targetHeight = Math.min(availableWidth, availableHeight);
            targetWidth = (int) (targetHeight * aspectRatio);
        }

        // 화면 크기 초과 방지
        if (targetWidth > availableWidth) {
            targetWidth = availableWidth;
            targetHeight = (int) (targetWidth / aspectRatio);
        }
        if (targetHeight > availableHeight) {
            targetHeight = availableHeight;
            targetWidth = (int) (targetHeight * aspectRatio);
        }

        // Draw_OriImage 크기 조정
        ViewGroup.MarginLayoutParams oriParams = (ViewGroup.MarginLayoutParams) Draw_OriImage.getLayoutParams();
        oriParams.width = targetWidth;
        oriParams.height = targetHeight;
        oriParams.setMargins(margin, margin, margin, margin);
        Draw_OriImage.setLayoutParams(oriParams);

        // Draw_MyImage 크기 조정
        ViewGroup.MarginLayoutParams myImageParams = (ViewGroup.MarginLayoutParams) Draw_MyImage.getLayoutParams();
        myImageParams.width = targetWidth;
        myImageParams.height = targetHeight;
        myImageParams.setMargins(margin, margin, margin, margin);
        Draw_MyImage.setLayoutParams(myImageParams);

        // Draw_dot 크기 조정
        ViewGroup.MarginLayoutParams dotParams = (ViewGroup.MarginLayoutParams) Draw_dot.getLayoutParams();
        dotParams.width = targetWidth;
        dotParams.height = targetHeight;
        dotParams.setMargins(margin, margin, margin, margin);
        Draw_OriImage.setLayoutParams(dotParams);

        Log.d("DrawFragment", "조정된 크기 - OriImage: " + targetWidth + "x" + targetHeight + ", Margin: " + margin);
        Log.d("DrawFragment", "조정된 크기 - MyImage: " + targetWidth + "x" + targetHeight + ", Margin: " + margin);

        Draw_MyImage.resumeDrawing(); // 그리기 시작 가능

    }

    private boolean checkCollision() {
        // Bitmap 가져오기
        userBitmap = Draw_MyImage.getCanvasBitmap();
        oriBitmap = ((BitmapDrawable) Draw_OriImage.getDrawable()).getBitmap();

        if (oriBitmap == null || userBitmap == null) {
            Log.e("DrawFragment", "비트맵이 null입니다!");
            return true; // 기본적으로 충돌로 간주
        }

        // Mat 변환
        oriMat = new Mat();
        userMat = new Mat();
        Utils.bitmapToMat(oriBitmap, oriMat);
        Utils.bitmapToMat(userBitmap, userMat);

        // 크기 조정 (oriMat 크기에 맞춤)
        if (!oriMat.size().equals(userMat.size())) {
            Imgproc.resize(userMat, userMat, oriMat.size());
            Log.d("DrawFragment", "크기 조정 완료: " + userMat.size());
        }

        // 타입 변환 (oriMat 타입에 맞춤)
        if (oriMat.type() != userMat.type()) {
            userMat.convertTo(userMat, oriMat.type());
            Log.d("DrawFragment", "타입 변환 완료: " + userMat.type());
        }

        // 이진화
        Imgproc.cvtColor(oriMat, oriMat, Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(oriMat, oriMat, 1, 255, Imgproc.THRESH_BINARY);

        Imgproc.cvtColor(userMat, userMat, Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(userMat, userMat, 1, 255, Imgproc.THRESH_BINARY_INV);

        Log.d("DrawFragment", "이진화 후 oriMat 크기: " + oriMat.size() + ", 값: " + Core.countNonZero(oriMat));
        Log.d("DrawFragment", "이진화 후 userMat 크기: " + userMat.size() + ", 값: " + Core.countNonZero(userMat));


        // 흰색 제거를 위한 마스크 생성
        Mat maskOri = new Mat();
        Mat maskUser = new Mat();
        Imgproc.threshold(oriMat, maskOri, 0, 255, Imgproc.THRESH_BINARY); // 흰색 제거
        Imgproc.threshold(userMat, maskUser, 0, 255, Imgproc.THRESH_BINARY); // 흰색 제거

        // 검은 선만 남긴 Mat 생성
        Mat filteredOriMat = new Mat();
        Mat filteredUserMat = new Mat();
        Core.bitwise_and(oriMat, maskOri, filteredOriMat);
        Core.bitwise_and(userMat, maskUser, filteredUserMat);

        // 겹치는 영역 계산
        Mat overlapMat = new Mat();
        Core.bitwise_and(oriMat, userMat, overlapMat);

        // 겹치는 픽셀 수 계산
        int overlappingPixels = Core.countNonZero(overlapMat);
        Log.d("DrawFragment", "겹치는 픽셀 수: " + overlappingPixels);

        // 디버그 이미지 저장
        saveDebugImages(oriMat, userMat, overlapMat);

        // 충돌 여부 판단
        int threshold = 400; // 허용치
        boolean isCollision = overlappingPixels > threshold;

        Log.d("DrawFragment", "충돌 여부: " + isCollision);

        // 메모리 해제
        oriMat.release();
        userMat.release();
        overlapMat.release();

        return isCollision;
    }


    private void saveDebugImages(Mat oriMat, Mat userMat, Mat overlapMat) {
        try {
            File debugDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "DebugImages");
            if (!debugDir.exists() && !debugDir.mkdirs()) {
                Log.e("DrawFragment", "디렉토리 생성 실패: " + debugDir.getAbsolutePath());
                return;
            }

            saveMatAsBitmap(oriMat, new File(debugDir, "oriMat_debug.jpg"));
            saveMatAsBitmap(userMat, new File(debugDir, "userMat_debug.jpg"));
            saveMatAsBitmap(overlapMat, new File(debugDir, "overlapMat_debug.jpg"));
        } catch (Exception e) {
            Log.e("DrawFragment", "디버그 이미지 저장 중 오류", e);
        }
    }

    private void saveMatAsBitmap(Mat mat, File file) {
        try {
            // Mat을 Bitmap으로 변환
            Bitmap bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mat, bitmap);

            // 디렉토리가 없는 경우 생성
            File parentDir = file.getParentFile();
            if (!parentDir.exists() && !parentDir.mkdirs()) {
                Log.e("DrawFragment", "디렉토리 생성 실패: " + parentDir.getAbsolutePath());
                return;
            }

            // 파일이 이미 존재하면 덮어쓰기 진행
            try (FileOutputStream fos = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos); // 기존 파일 덮어쓰기
                Log.d("DrawFragment", "Bitmap 저장 성공: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.e("DrawFragment", "Bitmap 저장 중 오류 발생", e);
        }
    }

}


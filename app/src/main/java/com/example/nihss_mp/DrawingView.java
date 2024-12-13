package com.example.nihss_mp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;

public class DrawingView extends View {

    private Path drawPath;
    private Paint drawPaint, canvasPaint;
    private Canvas drawCanvas;
    private Bitmap canvasBitmap;
    private boolean isDrawing = false; // 현재 그리기 상태를 확인하는 변수
    public boolean isDrawingEnabled = true; // 그림 그리기 활성화 상태를 확인하는 변수

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupDrawing();
    }

    private void setupDrawing() {
        drawPath = new Path();
        drawPaint = new Paint();
        drawPaint.setColor(Color.BLACK); // 검정색으로 설정하여 대비를 높임
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(20); // 두께를 증가시켜 특징점 인식을 높임
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
        canvasPaint = new Paint(Paint.DITHER_FLAG);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);

        // 흰색 배경으로 초기화
        drawCanvas.drawColor(Color.WHITE);
        Log.d("DrawingView", "Canvas 초기화 완료: " + w + "x" + h);

        // 첫 픽셀 색상 로그
        Log.d("DrawingView", "첫 픽셀 색상: " + canvasBitmap.getPixel(0, 0));
    }

    public void setCanvasBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            canvasBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            drawCanvas = new Canvas(canvasBitmap);
            invalidate(); // View 업데이트
            Log.d("DrawingView", "CanvasBitmap 크기 설정 완료: "
                    + canvasBitmap.getWidth() + "x" + canvasBitmap.getHeight());
        } else {
            Log.e("DrawingView", "설정하려는 Bitmap이 null입니다!");
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 기존 내용을 그립니다.
        canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);

        // 현재 경로를 그립니다.
        canvas.drawPath(drawPath, drawPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isDrawingEnabled) {
            return false; // 그림 그리기가 비활성화된 경우 입력 무시
        }

        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isDrawing = true; // 그림이 시작되었음을 표시
                drawPath.moveTo(touchX, touchY);
                //Log.d("DrawingView", "ACTION_DOWN: " + touchX + ", " + touchY);
                break;
            case MotionEvent.ACTION_MOVE:
                drawPath.lineTo(touchX, touchY);
                //Log.d("DrawingView", "ACTION_MOVE: " + touchX + ", " + touchY);
                break;
            case MotionEvent.ACTION_UP:
                isDrawing = false; // 그림이 멈춤을 표시
                drawCanvas.drawPath(drawPath, drawPaint); // canvasBitmap에 그림 저장
                drawPath.reset();
                //Log.d("DrawingView", "ACTION_UP: 그림 저장 완료");
                break;
            default:
                return false;
        }
        invalidate(); // 화면 업데이트
        return true;
    }

    public void saveCanvasBitmapDebug() {
        try {
            // 디버그 디렉터리 생성 (다운로드 폴더)
            File debugDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "DebugImages");
            if (!debugDir.exists()) debugDir.mkdirs();

            // 파일 이름 설정
            String fileName = "CanvasBitmap_Debug_" + System.currentTimeMillis() + ".png";
            File file = new File(debugDir, fileName);

            // Bitmap 저장
            FileOutputStream fos = new FileOutputStream(file);
            canvasBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            Log.d("DrawingView", "CanvasBitmap 저장 경로: " + file.getAbsolutePath());
        } catch (Exception e) {
            Log.e("DrawingView", "CanvasBitmap 저장 중 오류", e);
        }
    }

    // 현재 그리기 상태를 반환하는 메서드
    public boolean isDrawing() {
        return isDrawing;
    }

    // 사용자가 그린 내용이 없는 빈 상태인지 확인하는 메서드
    public boolean isCanvasBlank() {
        Bitmap blankBitmap = Bitmap.createBitmap(canvasBitmap.getWidth(), canvasBitmap.getHeight(), canvasBitmap.getConfig());
        Canvas blankCanvas = new Canvas(blankBitmap);
        blankCanvas.drawColor(Color.TRANSPARENT);

        return canvasBitmap.sameAs(blankBitmap); // canvasBitmap이 비어 있으면 true 반환
    }

    public Bitmap getCanvasBitmap() {
        Log.d("DrawingView", "getCanvasBitmap 호출됨 - Bitmap 크기: "
                + canvasBitmap.getWidth() + "x" + canvasBitmap.getHeight());
        int pixelColor = canvasBitmap.getPixel(10, 10); // 예시로 특정 픽셀 확인
        Log.d("DrawingView", "픽셀(10,10) 색상: " + pixelColor);
        return canvasBitmap;
    }

    // 그림 그리기 멈추기
    public void stopDrawing() {
        isDrawingEnabled = false;
    }

    // 그림 그리기 재개
    public void resumeDrawing() {
        isDrawingEnabled = true;
    }
}


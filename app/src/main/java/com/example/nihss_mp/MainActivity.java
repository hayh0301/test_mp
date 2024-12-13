package com.example.nihss_mp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.nihss_mp.NIHSS.NihssActivity;

public class MainActivity extends AppCompatActivity {

    Button BTN_StartNIHSS;
    Button BTN_CheckResult;
    Button BTN_QnA;
    final int PERMISSION = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //권한 확인
        CheckPermission();
        //버튼 클릭 이벤트 처리
        BTN_StartNIHSS = findViewById(R.id.BTN_StartNIHSS);
//        BTN_CheckResult = findViewById(R.id.BTN_CheckResult);
//        BTN_QnA = findViewById(R.id.BTN_QnA);
        BTN_StartNIHSS.setOnClickListener(new View.OnClickListener() {@Override public void onClick(View view) {
            Intent intent = new Intent(MainActivity.this, NihssActivity.class);startActivity(intent);}});
//        BTN_CheckResult.setOnClickListener(new View.OnClickListener() {@Override public void onClick(View view) {
//            Intent intent = new Intent(MainActivity.this, Check_NIHSS_Activity.class);startActivity(intent); finish();}});
//        BTN_QnA.setOnClickListener(new View.OnClickListener() {@Override public void onClick(View view) {
//            Intent intent = new Intent(MainActivity.this, QnA_NIHSS_Activity.class);startActivity(intent); finish();}});

    }

    void CheckPermission(){ //권한 확인
        // STT
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION);} else { }

        // cameraX(video)
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION);} else { }

        // vibrator
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.VIBRATE}, PERMISSION);} else { }

    }


}
package com.example.fec;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;

public class splashActivity extends AppCompatActivity {

    TextView tv;
    LottieAnimationView aniview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        aniview = (LottieAnimationView) findViewById(R.id.animation_view2);
        aniview.enableMergePathsForKitKatAndAbove(true);

        tv = findViewById(R.id.logger);
        tv.animate().translationYBy(60).setDuration(2000);
        tv.animate().alpha(1f).setDuration(2200);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(splashActivity.this, MainActivity.class));
                finish();
            }
        }, 2350);
    }

}

package com.huday.overwatchdrone;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {
    //location service
    final int REQUSET = 810;
    final private static int MY_PERMISSION_ACCESS_COURSE_LOCATION = 114;
    final private static int MY_PERMISSION_ACCESS_FINE_LOCATION = 514;
    final private static int MY_PERMISSION_ACCESS_BACKGROUND_LOCATION = 1919;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Ensure the map premission
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSION_ACCESS_COURSE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSION_ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    MY_PERMISSION_ACCESS_BACKGROUND_LOCATION);
        }

        final ImageView flyImage = (ImageView) this.findViewById(R.id.flyView);
        final ImageView settingImage = (ImageView) this.findViewById(R.id.settingView);

        flyImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, dronesAutoActivity.class);
                startActivity(intent);
            }
        });

        settingImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,wifiDirectActivity.class);
                startActivity(intent);
            }
        });
    }
}
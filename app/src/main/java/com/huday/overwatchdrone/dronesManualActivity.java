package com.huday.overwatchdrone;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class dronesManualActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drone_manual);

        final FloatingActionButton switchButton = (FloatingActionButton) this.findViewById(R.id.switchButton2);

        switchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(dronesManualActivity.this, dronesAutoActivity.class);
                startActivity(intent);
            }
        });


    }

    public boolean onKeyDown(int keyCode, KeyEvent event){
        if (keyCode == KeyEvent.KEYCODE_BACK){
            Intent home = new Intent(dronesManualActivity.this,MainActivity.class);
            //home.setFlags((Intent.FLAG_ACTIVITY_CLEAR_TOP));
            startActivity((home));
            return true;
        }
        return super.onKeyDown(keyCode,event);
    }
}
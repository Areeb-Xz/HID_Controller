package com.example.hidcontroller;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

public class ModeSelectActivity extends AppCompatActivity {

    private static final String TAG = "ModeSelectActivity";
    private TextView titleText;
    private TextView modeStatus;
    private TextView footerText;
    private Button btnKeyboardMode;
    private Button btnTouchpadMode;
    private Button btnCustomMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mode_select);
        // Initialize views with CORRECT IDs from your XML
        titleText = findViewById(R.id.titleText);
        modeStatus = findViewById(R.id.modeStatus);
        footerText = findViewById(R.id.footerText);
        btnKeyboardMode = findViewById(R.id.btnKeyboardMode);
        btnTouchpadMode = findViewById(R.id.btnTouchpadMode);
        btnCustomMode = findViewById(R.id.btnCustomMode);
        String deviceName = getIntent().getStringExtra("deviceName");
        if (deviceName != null) {
            modeStatus.setText("Connected to: " + deviceName + ". Choose a mode.");
        } else {
            modeStatus.setText("Connected. Choose a mode.");
        }

        // Keyboard Mode button
        btnKeyboardMode.setOnClickListener(v -> {
            Log.d(TAG, "Launching Keyboard Mode");
            Intent intent = new Intent(ModeSelectActivity.this, KeyboardActivity.class);
            startActivity(intent);
        });

        // Touchpad Mode button
        btnTouchpadMode.setOnClickListener(v -> {
            Log.d(TAG, "Launching Touchpad Mode");
            Intent intent = new Intent(ModeSelectActivity.this, TouchpadActivity.class);
            startActivity(intent);
        });

        // Custom Mode button (disabled)
        btnCustomMode.setOnClickListener(v -> {
            Log.d(TAG, "Custom Mode not yet available");
            modeStatus.setText("Custom Mode coming soon!");
        });

        Log.d(TAG, "Activity created");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.d(TAG, "Back pressed, returning to connection screen");
    }
}
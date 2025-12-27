package com.example.hidcontroller;

import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class TouchpadSettingsActivity extends AppCompatActivity {

    private static final String TAG = "TouchpadSettings";
    private static final String PREF_FILE = "touchpad_prefs";
    private static final String PREF_TOUCHPAD_SENSITIVITY = "touchpad_sensitivity";
    private static final String PREF_SCROLL_SENSITIVITY = "scroll_sensitivity";

    private SeekBar seekBarTouchpadSensitivity;
    private SeekBar seekBarScrollSensitivity;
    private TextView tvTouchpadValue;
    private TextView tvScrollValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_touchpad_settings);

        seekBarTouchpadSensitivity = findViewById(R.id.seekBarTouchpadSensitivity);
        seekBarScrollSensitivity = findViewById(R.id.seekBarScrollSensitivity);
        tvTouchpadValue = findViewById(R.id.tvTouchpadValue);
        tvScrollValue = findViewById(R.id.tvScrollValue);

        // Load saved values (0–20, default 10)
        int savedTouch = getSharedPreferences(PREF_FILE, MODE_PRIVATE)
                .getInt(PREF_TOUCHPAD_SENSITIVITY, 10);
        int savedScroll = getSharedPreferences(PREF_FILE, MODE_PRIVATE)
                .getInt(PREF_SCROLL_SENSITIVITY, 10);

        seekBarTouchpadSensitivity.setMax(20);
        seekBarScrollSensitivity.setMax(20);

        seekBarTouchpadSensitivity.setProgress(savedTouch);
        seekBarScrollSensitivity.setProgress(savedScroll);

        tvTouchpadValue.setText(String.valueOf(savedTouch));
        tvScrollValue.setText(String.valueOf(savedScroll));

        seekBarTouchpadSensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int value, boolean fromUser) {
                tvTouchpadValue.setText(String.valueOf(value));
                saveInt(PREF_TOUCHPAD_SENSITIVITY, value);
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {}

            @Override
            public void onStopTrackingTouch(SeekBar sb) {}
        });

        seekBarScrollSensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int value, boolean fromUser) {
                tvScrollValue.setText(String.valueOf(value));
                saveInt(PREF_SCROLL_SENSITIVITY, value);
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {}

            @Override
            public void onStopTrackingTouch(SeekBar sb) {}
        });

        Log.d(TAG, "Settings Activity Created");
    }

    private void saveInt(String key, int value) {
        getSharedPreferences(PREF_FILE, MODE_PRIVATE)
                .edit()
                .putInt(key, value)
                .apply();
        Log.d(TAG, "Saved " + key + " = " + value);
    }

    /**
     * Get the touchpad sensitivity multiplier (0.5x to 2.0x)
     * 0 = 0.5x (slowest), 10 = 1.0x (normal), 20 = 2.0x (fastest)
     */
    public static float getTouchpadSensitivityMultiplier(AppCompatActivity ctx) {
        int value = ctx.getSharedPreferences(PREF_FILE, MODE_PRIVATE)
                .getInt(PREF_TOUCHPAD_SENSITIVITY, 10);
        // Formula: 0.5 + (value * 0.075) = 0.5 to 2.0 range
        float multiplier = 0.5f + (value * 0.075f);
        Log.d(TAG, "Touchpad multiplier: " + multiplier + " (value=" + value + ")");
        return multiplier;
    }

    /**
     * Get the scroll sensitivity multiplier (0.5x to 2.0x)
     * 0 = 0.5x (slowest), 10 = 1.0x (normal), 20 = 2.0x (fastest)
     */
    public static float getScrollSensitivityMultiplier(AppCompatActivity ctx) {
        int value = ctx.getSharedPreferences(PREF_FILE, MODE_PRIVATE)
                .getInt(PREF_SCROLL_SENSITIVITY, 10);
        // Formula: 0.5 + (value * 0.075) = 0.5 to 2.0 range
        float multiplier = 0.5f + (value * 0.075f);
        Log.d(TAG, "Scroll multiplier: " + multiplier + " (value=" + value + ")");
        return multiplier;
    }
}
package com.example.hidcontroller;
import android.content.Intent;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Bundle;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.view.MotionEvent;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

public class KeyboardActivity extends AppCompatActivity {
    private static final String TAG = "KeyboardActivity";
    private GridLayout keyboardGrid;
    private TextView keyboardStatus;
    private BluetoothHIDService hidService;
    private boolean isBound = false;
    private boolean isShiftActive = false;


    // 50-key QWERTY keyboard layout (5x10 grid)
    private String[][] keyboardLayout = {
            {"Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"},
            {"A", "S", "D", "F", "G", "H", "J", "K", "L", ";"},
            {"Z", "X", "C", "V", "B", "N", "M", ",", ".", "/"},
            {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"},
            {"-", "=", "[", "]", "\\", "'", "`", "Shift", "Space", "Back", "Enter"}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keyboard);

        keyboardGrid = findViewById(R.id.keyboardGrid);
        keyboardStatus = findViewById(R.id.keyboardStatus);

        createKeyboardButtons();
        logToStatus("Keyboard initialized");

        Intent serviceIntent = new Intent(this, BluetoothHIDService.class);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    private void createKeyboardButtons() {
        keyboardGrid.removeAllViews();

        for (int row = 0; row < keyboardLayout.length; row++) {
            for (int col = 0; col < keyboardLayout[row].length; col++) {

                final String keyLabel = keyboardLayout[row][col];

                Button keyButton = new Button(this);
                keyButton.setText(keyLabel);
                keyButton.setTag(keyLabel);

                // Use Spec weights (supported) so the grid distributes space evenly.
                GridLayout.Spec rowSpec = GridLayout.spec(row, 1f);
                GridLayout.Spec colSpec = GridLayout.spec(col, 1f);

                GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowSpec, colSpec);
                params.width = 0;   // width becomes proportional due to weight
                params.height = 0;  // height becomes proportional due to weight
                params.setMargins(2, 2, 2, 2);

                keyButton.setLayoutParams(params);
                keyButton.setTextSize(10);
                keyButton.setBackgroundColor(0xFF404040);
                keyButton.setTextColor(0xFFFFFFFF);

                keyButton.setOnTouchListener((v, event) -> {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        onKeyDown(keyLabel, keyButton);
                        return true;
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        onKeyUp(keyLabel, keyButton);
                        v.performClick(); // fixes accessibility lint warning
                        return true;
                    }
                    return false;
                });

                keyboardGrid.addView(keyButton);
            }
        }
    }

    private void onKeyDown(String keyLabel, Button button) {
        button.setBackgroundColor(0xFF606060);
        if ("Shift".equals(keyLabel)) {
            // Just visual feedback; actual toggle happens on UP
            logToStatus("Shift pressed");
        } else {
            logToStatus("Key DOWN: " + keyLabel);
            Log.d(TAG, "Key pressed: " + keyLabel);
        }
    }

    private Button shiftButton; // field in class
    private void onKeyUp(String keyLabel, Button button) {
        button.setBackgroundColor(0xFF404040);

        if ("Shift".equals(keyLabel)) {
            // toggle shift
            isShiftActive = !isShiftActive;
            shiftButton = button; // remember it
            shiftButton.setBackgroundColor(isShiftActive ? 0xFF808080 : 0xFF404040);
            return;
        }

        // normal key
        logToStatus("Key UP: " + keyLabel);

        if (hidService != null && hidService.isConnected()) {
            byte modifier = isShiftActive ? (byte) 0x02 : (byte) 0x00;
            hidService.sendKeyPress(keyLabel, modifier);  // sends key
            Log.d(TAG, "Sent key via HID: " + keyLabel + " modifier=" + modifier);
        }

        // turn shift off AFTER sending one key
        if (isShiftActive) {
            isShiftActive = false;
            if (shiftButton != null) {
                shiftButton.setBackgroundColor(0xFF404040);
            }
            logToStatus("Shift OFF");
        }
    }

    private void logToStatus(String message) {
        keyboardStatus.setText(message);
    }

    @Override
    public void onBackPressed() {
        logToStatus("Back pressed");
        super.onBackPressed();
    }
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BluetoothHIDService.LocalBinder binder = (BluetoothHIDService.LocalBinder) service;
            hidService = binder.getService();
            isBound = true;
            logToStatus("HID Service ready");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            hidService = null;
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }
}
package com.example.hidcontroller;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class TouchpadActivity extends AppCompatActivity {

    private static final String TAG = "TouchpadActivity";
    // Service reference
    private BluetoothHIDService hidService;
    private boolean isBound = false;
    // UI components
    private View touchArea;
    private View scrollStrip;
    private Button btnLeftClick;
    private Button btnRightClick;
    private ImageButton btnScrollUp;
    private ImageButton btnScrollDown;
    private TextView statusText;
    // Touch tracking
    private float lastX = 0f;
    private float lastY = 0f;
    private float lastScrollY = 0f;
    private static final int MOVEMENT_THRESHOLD = 2;
    private static final int SCROLL_MULTIPLIER = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_touchpad);
        Log.d(TAG, "TouchpadActivity created");

        initializeUI();
        bindToHIDService();
    }

    // ---------- UI setup ----------
    private void initializeUI() {
        touchArea = findViewById(R.id.touchArea);
        scrollStrip = findViewById(R.id.scrollStrip);
        btnLeftClick = findViewById(R.id.btnLeftClick);
        btnRightClick = findViewById(R.id.btnRightClick);
        btnScrollUp = findViewById(R.id.btnScrollUp);
        btnScrollDown = findViewById(R.id.btnScrollDown);
        statusText = findViewById(R.id.touchpadStatus);

        touchArea.setOnTouchListener(this::onTouchAreaEvent);

        btnLeftClick.setOnClickListener(v -> onLeftClickPressed());
        btnRightClick.setOnClickListener(v -> onRightClickPressed());

        btnScrollUp.setOnClickListener(v -> onScrollUp());
        btnScrollDown.setOnClickListener(v -> onScrollDown());

        scrollStrip.setOnTouchListener((v, event) -> {
            float y = event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastScrollY = y;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dy = y - lastScrollY;
                    if (Math.abs(dy) > MOVEMENT_THRESHOLD &&
                            hidService != null && hidService.isConnected()) {
                        int direction = dy < 0 ? SCROLL_MULTIPLIER : -SCROLL_MULTIPLIER;
                        hidService.sendMouseScroll(direction);
                        logToStatus(direction > 0 ? "↑ Scrolling up" : "↓ Scrolling down");
                        lastScrollY = y;
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    return true;
            }
            return false;
        });

        logToStatus("Touchpad initialized. Waiting for connection...");
    }

    // ---------- Touchpad movement ----------
    private boolean onTouchAreaEvent(View v, MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastX = x;
                lastY = y;
                logToStatus("Touchpad active");
                return true;

            case MotionEvent.ACTION_MOVE:
                int deltaX = (int) (x - lastX);
                int deltaY = (int) (y - lastY);
                if (Math.abs(deltaX) > MOVEMENT_THRESHOLD ||
                        Math.abs(deltaY) > MOVEMENT_THRESHOLD) {
                    sendMouseMovement(deltaX, deltaY);
                    lastX = x;
                    lastY = y;
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                logToStatus("Touchpad ready");
                return true;
        }
        return false;
    }

    private void sendMouseMovement(int deltaX, int deltaY) {
        if (hidService == null || !hidService.isConnected()) {
            logToStatus("⚠ Not connected to device");
            return;
        }
        Log.d(TAG, "Mouse move: deltaX=" + deltaX + ", deltaY=" + deltaY);
        logToStatus("Move: X=" + deltaX + ", Y=" + deltaY);
        hidService.sendMouseMovement(deltaX, deltaY);
    }

    // ---------- Click / scroll actions ----------
    private void onLeftClickPressed() {
        if (hidService == null || !hidService.isConnected()) {
            logToStatus("⚠ Not connected to device");
            return;
        }
        Log.d(TAG, "Left click pressed");
        logToStatus("Left click");
        hidService.sendMouseClick(0x01);
    }

    private void onRightClickPressed() {
        if (hidService == null || !hidService.isConnected()) {
            logToStatus("⚠ Not connected to device");
            return;
        }
        Log.d(TAG, "Right click pressed");
        logToStatus("Right click");
        hidService.sendMouseClick(0x02);
    }

    private void onScrollUp() {
        if (hidService == null || !hidService.isConnected()) {
            logToStatus("⚠ Not connected to device");
            return;
        }
        Log.d(TAG, "Scroll up");
        logToStatus("↑ Scrolling up");
        hidService.sendMouseScroll(SCROLL_MULTIPLIER);
    }

    private void onScrollDown() {
        if (hidService == null || !hidService.isConnected()) {
            logToStatus("⚠ Not connected to device");
            return;
        }
        Log.d(TAG, "Scroll down");
        logToStatus("↓ Scrolling down");
        hidService.sendMouseScroll(-SCROLL_MULTIPLIER);
    }

    private void logToStatus(String message) {
        statusText.setText(message);
        Log.d(TAG, message);
    }

    // ---------- Service binding ----------
    private void bindToHIDService() {
        Intent serviceIntent = new Intent(this, BluetoothHIDService.class);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BluetoothHIDService.LocalBinder binder =
                    (BluetoothHIDService.LocalBinder) service;
            hidService = binder.getService();
            isBound = true;

            if (hidService.isConnected()) {
                logToStatus("✓ Connected to device. Ready!");
            } else {
                logToStatus("Service ready. No device connected.");
            }
            Log.d(TAG, "HID Service bound");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            hidService = null;
            logToStatus("⚠ HID Service disconnected");
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        Log.d(TAG, "onDestroy()");
    }

    @Override
    public void onBackPressed() {
        logToStatus("Returning to main screen");
        super.onBackPressed();
    }
}
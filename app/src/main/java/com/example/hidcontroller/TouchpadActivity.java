package com.example.hidcontroller;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class TouchpadActivity extends AppCompatActivity {

    private static final String TAG = "TouchpadActivity";

    private TextView touchpadStatus;
    private View touchArea;
    private View scrollStrip;
    private Button btnLeftClick;
    private Button btnRightClick;
    private ImageButton btnScrollUp;
    private ImageButton btnScrollDown;

    @Nullable
    private BluetoothHIDService hidService;
    private boolean isBound = false;

    private float lastX = 0f;
    private float lastY = 0f;
    private float lastScrollY = 0f;
    private float downX = 0f;
    private float downY = 0f;
    private long downTime = 0L;

    private static final int TAP_SLOP_PX = 20;      // adjust if needed
    private static final int TAP_TIMEOUT_MS = 180;  // adjust if needed
    private boolean moved = false;
    private static final int MOVEMENT_THRESHOLD = 2;
    private static final int SCROLL_MULTIPLIER = 3;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BluetoothHIDService.LocalBinder binder = (BluetoothHIDService.LocalBinder) service;
            hidService = binder.getService();
            isBound = true;
            Log.d(TAG, "Service bound");
            updateStatus();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            hidService = null;
            Log.d(TAG, "Service disconnected");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_touchpad);

        touchpadStatus = findViewById(R.id.touchpadStatus);
        touchArea = findViewById(R.id.touchArea);
        scrollStrip = findViewById(R.id.scrollStrip);
        btnLeftClick = findViewById(R.id.btnLeftClick);
        btnRightClick = findViewById(R.id.btnRightClick);
        btnScrollUp = findViewById(R.id.btnScrollUp);
        btnScrollDown = findViewById(R.id.btnScrollDown);

        touchpadStatus.setText("Connecting to service...");

        Intent serviceIntent = new Intent(this, BluetoothHIDService.class);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);

        setupTouchpadUI();
        setupControlButtons();

        Log.d(TAG, "Activity created");
    }

    /**
     * Setup touchpad with proper separation of touch area and scroll strip
     * Left side: mouse movement
     * Right side: scrolling only
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setupTouchpadUI() {
        // MAIN TOUCHPAD AREA - left side for mouse movement
        touchArea.setOnTouchListener((v, event) -> {
            if (hidService == null || !hidService.isConnected()) {
                touchpadStatus.setText("Not connected to device");
                return false;
            }

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = event.getX();
                    lastY = event.getY();

                    downX = lastX;
                    downY = lastY;
                    downTime = event.getEventTime();
                    moved = false;
                    break;

                case MotionEvent.ACTION_MOVE:
                    float currentX = event.getX();
                    float currentY = event.getY();

                    float deltaX = (currentX - lastX) * 1.5f;
                    float deltaY = (currentY - lastY) * 1.5f;

                    if (Math.abs(deltaX) > MOVEMENT_THRESHOLD || Math.abs(deltaY) > MOVEMENT_THRESHOLD) {
                        hidService.sendMouseMovement((int) deltaX, (int) deltaY);
                        moved = true;
                    }

                    lastX = currentX;
                    lastY = currentY;
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    long upTime = event.getEventTime();
                    float upX = event.getX();
                    float upY = event.getY();

                    boolean quick = (upTime - downTime) <= TAP_TIMEOUT_MS;
                    boolean smallMove = Math.abs(upX - downX) <= TAP_SLOP_PX && Math.abs(upY - downY) <= TAP_SLOP_PX;

                    if (!moved && quick && smallMove) {
                        hidService.sendMouseClick(1); // left click
                        logToStatus("Tap click");
                    }

                    lastX = 0f;
                    lastY = 0f;
                    break;
            }
            return true;
        });

        // SCROLL STRIP - right side for scrolling only
        scrollStrip.setOnTouchListener((v, event) -> {
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastScrollY = y;
                    logToStatus("Scroll mode");
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float dy = y - lastScrollY;

                    // Only respond to vertical movement
                    if (Math.abs(dy) > MOVEMENT_THRESHOLD) {
                        if (hidService != null && hidService.isConnected()) {
                            int direction = dy > 0 ? -SCROLL_MULTIPLIER : SCROLL_MULTIPLIER;
                            hidService.sendMouseScroll(direction);
                            logToStatus(direction > 0 ? "Scrolling up" : "Scrolling down");
                        }
                    }
                    lastScrollY = y;
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    logToStatus("Ready");
                    return true;
            }
            return false;
        });
    }

    @SuppressLint("MissingPermission")
    private void setupControlButtons() {
        // LEFT CLICK
        btnLeftClick.setOnClickListener(v -> {
            if (hidService != null && hidService.isConnected()) {
                hidService.sendMouseClick(1);
                logToStatus("Left click");
                Log.d(TAG, "Left click sent");
            } else {
                touchpadStatus.setText("Not connected to device");
            }
        });

        // RIGHT CLICK
        btnRightClick.setOnClickListener(v -> {
            if (hidService != null && hidService.isConnected()) {
                hidService.sendMouseClick(2);
                logToStatus("Right click");
                Log.d(TAG, "Right click sent");
            } else {
                touchpadStatus.setText("Not connected to device");
            }
        });

        // SCROLL UP BUTTON
        btnScrollUp.setOnClickListener(v -> {
            if (hidService != null && hidService.isConnected()) {
                hidService.sendMouseScroll(SCROLL_MULTIPLIER);
                logToStatus("Scrolling up");
                Log.d(TAG, "Scroll up sent");
            } else {
                touchpadStatus.setText("Not connected to device");
            }
        });

        // SCROLL DOWN BUTTON
        btnScrollDown.setOnClickListener(v -> {
            if (hidService != null && hidService.isConnected()) {
                hidService.sendMouseScroll(-SCROLL_MULTIPLIER);
                logToStatus("Scrolling down");
                Log.d(TAG, "Scroll down sent");
            } else {
                touchpadStatus.setText("Not connected to device");
            }
        });
    }

    private void logToStatus(String message) {
        touchpadStatus.setText(message);
        Log.d(TAG, message);
    }

    private void updateStatus() {
        if (hidService == null || !hidService.isConnected()) {
            touchpadStatus.setText("Not connected");
        } else {
            BluetoothDevice device = hidService.getConnectedDevice();
            @SuppressLint("MissingPermission")
            String deviceName = (device != null && device.getName() != null) ? device.getName() : "Unknown Device";
            touchpadStatus.setText("Connected to " + deviceName);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        Log.d(TAG, "Activity destroyed");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (hidService != null) {
            hidService.disconnect();
        }
    }
}
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
    // Tap and drag fields
    private boolean isDragging = false;
    private long downTime = 0L;
    private float downX = 0f;
    private float downY = 0f;
    private boolean moved = false;
    // Tap detection fields
    private float tapDownX = 0f;
    private float tapDownY = 0f;
    private long tapDownTime = 0L;
    private static final int MOVEMENT_THRESHOLD = 2;
    private static final int SCROLL_MULTIPLIER = 3;
    private static final int DRAG_START_MS = 250;      // Hold for 250ms to start drag
    private static final int DRAG_SLOP_PX = 20;        // Allow small movement while initiating drag
    private static final int TAP_TIMEOUT_MS = 180;     // Tap must complete within 180ms
    private static final int TAP_SLOP_PX = 20;         // Tap movement tolerance

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
            float currentX = event.getX();
            float currentY = event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = event.getX();
                    lastY = event.getY();

                    // Record down state for tap and drag detection
                    downX = lastX;
                    downY = lastY;
                    downTime = event.getEventTime();
                    tapDownX = lastX;
                    tapDownY = lastY;
                    tapDownTime = event.getEventTime();

                    isDragging = false;
                    moved = false;
                    logToStatus("Touch down");
                    break;

                case MotionEvent.ACTION_MOVE:

                    // Raw deltas for more precision (no multiplier)
                    float deltaX = currentX - lastX;
                    float deltaY = currentY - lastY;

                    // Check if we should start dragging
                    if (!isDragging && !moved) {
                        boolean heldLongEnough = (event.getEventTime() - downTime) >= DRAG_START_MS;
                        boolean smallMoveFromDown =
                                Math.abs(currentX - downX) <= DRAG_SLOP_PX &&
                                        Math.abs(currentY - downY) <= DRAG_SLOP_PX;

                        if (heldLongEnough && smallMoveFromDown) {
                            isDragging = true;
                            hidService.sendMouseReport((byte) 0x01, 0, 0, 0); // Left button down
                            logToStatus("Drag start");
                        }
                    }

                    // Send movement (with button held if dragging)
                    if (Math.abs(deltaX) > MOVEMENT_THRESHOLD || Math.abs(deltaY) > MOVEMENT_THRESHOLD) {
                        moved = true;

                        if (isDragging) {
                            // Move while holding left button
                            hidService.sendMouseReport((byte) 0x01, (int) deltaX, (int) deltaY, 0);
                            logToStatus("Drag X:" + (int)deltaX + " Y:" + (int)deltaY);
                        } else {
                            // Normal movement (no button)
                            hidService.sendMouseMovement((int) deltaX, (int) deltaY);
                            logToStatus("Move X:" + (int)deltaX + " Y:" + (int)deltaY);
                        }
                    }

                    lastX = currentX;
                    lastY = currentY;
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (isDragging) {
                        // Release button after drag
                        hidService.sendMouseReport((byte) 0x00, 0, 0, 0);
                        isDragging = false;
                        logToStatus("Drag end");
                    } else if (!moved) {
                        // Detect single tap (no movement)
                        long upTime = event.getEventTime();
                        float upX = currentX;
                        float upY = currentY;

                        boolean quick = (upTime - tapDownTime) <= TAP_TIMEOUT_MS;
                        boolean smallMove = Math.abs(upX - tapDownX) <= TAP_SLOP_PX &&
                                Math.abs(upY - tapDownY) <= TAP_SLOP_PX;

                        if (quick && smallMove) {
                            hidService.sendMouseClick(1); // Left click
                            logToStatus("Tap click");
                        }
                    }

                    lastX = 0f;
                    lastY = 0f;
                    moved = false;
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
                            // Inverted: swipe down = scroll down (positive), swipe up = scroll up (negative)
                            int direction = dy > 0 ? SCROLL_MULTIPLIER : -SCROLL_MULTIPLIER;
                            // Reduce speed: divide by 2
                            hidService.sendMouseScroll(direction / 2);
                            logToStatus(direction > 0 ? "Scrolling down" : "Scrolling up");
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
                logToStatus("Left click (button)");
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
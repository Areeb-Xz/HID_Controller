package com.example.hidcontroller;
import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * MainActivity
 *
 * The main screen of the HID Controller app.
 * Handles:
 * - Runtime permissions (Bluetooth, Camera)
 * - UI setup
 * - User interaction (buttons)
 * - Navigation to other screens (keyboard, touchpad, etc.)
 *
 * Lifecycle:
 * 1. onCreate() - called when Activity first loads
 * 2. onStart() - screen is about to be visible
 * 3. onResume() - screen is now visible and active
 * 4. onPause() - screen about to leave
 * 5. onDestroy() - activity closing
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    // Permission constants
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.CAMERA};
    private TextView statusTextView;
    private Button keyboardButton;
    private Button touchpadButton;
    private Button settingsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load UI layout from activity_main.xml
        setContentView(R.layout.activity_main);
        Log.d(TAG, "MainActivity created");
        // Initialize UI components
        initializeUI();
        // Request permissions if needed
        requestRequiredPermissions();
    }

    private void initializeUI() {
        statusTextView = findViewById(R.id.status_text);
        keyboardButton = findViewById(R.id.keyboard_button);
        touchpadButton = findViewById(R.id.touchpad_button);
        settingsButton = findViewById(R.id.settings_button);
        // Set click listeners
        keyboardButton.setOnClickListener(v -> onKeyboardClicked());
        touchpadButton.setOnClickListener(v -> onTouchpadClicked());
        settingsButton.setOnClickListener(v -> onSettingsClicked());

        updateStatusText("App initialized. Ready to connect.");
    }

    private void requestRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {  // Android 12+
            boolean allPermissionsGranted = true;

            for (String permission : REQUIRED_PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(this, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (!allPermissionsGranted) {
                // Ask user for permissions
                ActivityCompat.requestPermissions(
                        this,
                        REQUIRED_PERMISSIONS,
                        PERMISSION_REQUEST_CODE
                );
            } else {
                Log.d(TAG, "All permissions already granted");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;

            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                Log.d(TAG, "All permissions granted by user");
                updateStatusText("Permissions granted. Ready to proceed.");
            } else {
                Log.e(TAG, "Some permissions denied");
                updateStatusText("Permissions denied. App may not work properly.");
            }
        }
    }

    private void onKeyboardClicked() {
        Log.d(TAG, "Keyboard button clicked");
        Intent intent = new Intent(MainActivity.this, KeyboardActivity.class);
        startActivity(intent);
    }

    private void onTouchpadClicked() {
        Log.d(TAG, "Touchpad button clicked");
        Intent intent = new Intent(MainActivity.this, TouchpadActivity.class);
        startActivity(intent);
    }

    private void onSettingsClicked() {
        Log.d(TAG, "Settings button clicked");
        Intent intent = new Intent(MainActivity.this, BluetoothConnectionActivity.class);
        startActivity(intent);
    }

    private void updateStatusText(String message) {
        statusTextView.setText(message);
        Log.d(TAG, message);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "MainActivity onStart()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "MainActivity onResume()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "MainActivity onPause()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MainActivity onDestroy()");
    }
}
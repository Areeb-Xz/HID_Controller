package com.example.hidcontroller;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final String[] BLUETOOTH_PERMISSIONS = {
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button connectButton = findViewById(R.id.btnConnectDevice);
        connectButton.setOnClickListener(v -> {
            Log.d(TAG, "Connect button clicked");

            // Check and request permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ requires runtime permission check
                if (hasBluetoothPermissions()) {
                    startBluetoothConnection();
                } else {
                    requestBluetoothPermissions();
                }
            } else {
                // Android 11 and below
                startBluetoothConnection();
            }
        });

        Log.d(TAG, "MainActivity created");
    }

    /**
     * Check if all required Bluetooth permissions are granted
     */
    private boolean hasBluetoothPermissions() {
        for (String permission : BLUETOOTH_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Request Bluetooth permissions from the user
     */
    private void requestBluetoothPermissions() {
        ActivityCompat.requestPermissions(
                this,
                BLUETOOTH_PERMISSIONS,
                PERMISSION_REQUEST_CODE
        );
    }

    /**
     * Start the Bluetooth connection activity
     */
    private void startBluetoothConnection() {
        Intent intent = new Intent(MainActivity.this, BluetoothConnectionActivity.class);
        startActivity(intent);
    }

    /**
     * Handle permission request result
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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
                Log.d(TAG, "Bluetooth permissions granted");
                startBluetoothConnection();
            } else {
                Toast.makeText(this, "Bluetooth permissions are required to connect", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Bluetooth permissions denied");
            }
        }
    }
}
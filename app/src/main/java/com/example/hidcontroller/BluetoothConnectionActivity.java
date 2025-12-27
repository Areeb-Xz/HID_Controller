package com.example.hidcontroller;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class BluetoothConnectionActivity extends AppCompatActivity {

    private static final String TAG = "BluetoothConnectionActivity";
    private LinearLayout deviceListContainer;
    private TextView statusText;
    @Nullable
    private BluetoothHIDService hidService;
    private boolean isBound = false;
    private Button currentlyConnectingButton = null;
    private static final long CONNECTION_TIMEOUT_MS = 5000;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BluetoothHIDService.LocalBinder binder =
                    (BluetoothHIDService.LocalBinder) service;
            hidService = binder.getService();
            isBound = true;
            Log.d(TAG, "Service bound");
            loadPairedDevices();
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
        setContentView(R.layout.activity_bluetooth_connection);
        deviceListContainer = findViewById(R.id.deviceListContainer);
        statusText = findViewById(R.id.connectionStatusText);
        statusText.setText("Initializing...");
        Intent serviceIntent = new Intent(this, BluetoothHIDService.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);

        Log.d(TAG, "Activity created, service starting");
    }

    private void loadPairedDevices() {
        if (hidService == null) {
            statusText.setText("Error: HID Service not available");
            return;
        }

        BluetoothDevice[] pairedDevices = hidService.getPairedDevices();

        if (pairedDevices.length == 0) {
            statusText.setText("No paired devices found");
            return;
        }

        String countMsg = "Found " + pairedDevices.length + " device(s):";
        statusText.setText(countMsg);

        Log.d(TAG, countMsg);

        deviceListContainer.removeAllViews();

        for (BluetoothDevice device : pairedDevices) {
            Button deviceButton = createDeviceButton(device);
            deviceListContainer.addView(deviceButton);
        }
    }

    private Button createDeviceButton(BluetoothDevice device) {
        Button btn = new Button(this);

        @SuppressLint("MissingPermission") String name = device.getName() != null ? device.getName() : "Unknown";
        String addr = device.getAddress();
        btn.setText(name + "\n" + addr);
        btn.setPadding(16, 16, 16, 16);
        btn.setBackgroundColor(0xFF404040);
        btn.setTextColor(0xFFFFFFFF);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 8, 8, 8);
        btn.setLayoutParams(params);

        btn.setOnClickListener(v -> {
            if (currentlyConnectingButton != null) {
                statusText.setText("Already connecting... please wait");
                return;
            }

            currentlyConnectingButton = btn;
            btn.setEnabled(false);
            btn.setAlpha(0.5f);

            connectTo(device, btn);
        });

        return btn;
    }

    private void connectTo(BluetoothDevice device, Button btn) {
        if (hidService == null) {
            statusText.setText("Error: HID Service not available");
            btn.setEnabled(true);
            btn.setAlpha(1.0f);
            currentlyConnectingButton = null;
            return;
        }

        @SuppressLint("MissingPermission") String deviceName = device.getName() != null ? device.getName() : "Unknown";
        statusText.setText("Connecting to: " + deviceName + "...");

        Log.d(TAG, "Selected device: " + deviceName + " (" + device.getAddress() + ")");

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (currentlyConnectingButton == btn && !hidService.isConnected()) {
                Log.w(TAG, "Connection timeout for " + deviceName);
                statusText.setText("Connection timeout. Device may not support HID.");
                btn.setEnabled(true);
                btn.setAlpha(1.0f);
                currentlyConnectingButton = null;
            }
        }, CONNECTION_TIMEOUT_MS);

        hidService.connectToDevice(device, new BluetoothHIDService.ConnectionCallback() {
            @Override
            public void onConnected(BluetoothDevice device) {
                runOnUiThread(() -> {
                    @SuppressLint("MissingPermission") String name = device.getName() != null ?
                            device.getName() : "device";

                    statusText.setText("✓ Connected to: " + name);
                    Log.d(TAG, "Successfully connected to: " + name);

                    new android.os.Handler(android.os.Looper.getMainLooper())
                            .postDelayed(() -> {
                                Intent intent = new Intent(
                                        BluetoothConnectionActivity.this,
                                        ModeSelectActivity.class
                                );
                                intent.putExtra("deviceName", name);
                                startActivity(intent);
                                finish();
                            }, 500);
                });
            }

            @Override
            public void onError(@Nullable BluetoothDevice device, String message) {
                runOnUiThread(() -> {
                    @SuppressLint("MissingPermission") String name = (device != null) ?
                            (device.getName() != null ? device.getName() : device.getAddress())
                            : "device";

                    statusText.setText("✗ Failed to connect to " + name +
                            ": " + message);

                    Log.w(TAG, "Connection error for " + name + ": " + message);

                    btn.setEnabled(true);
                    btn.setAlpha(1.0f);
                    currentlyConnectingButton = null;
                });
            }
        });
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
}
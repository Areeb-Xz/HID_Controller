package com.example.hidcontroller;

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

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BluetoothHIDService.LocalBinder binder = (BluetoothHIDService.LocalBinder) service;
            hidService = binder.getService();
            isBound = true;
            loadPairedDevices();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            hidService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connection);

        deviceListContainer = findViewById(R.id.deviceListContainer);
        statusText = findViewById(R.id.connectionStatusText);
        statusText.setText("Loading paired devices...");

        Intent serviceIntent = new Intent(this, BluetoothHIDService.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
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

        statusText.setText("Found " + pairedDevices.length + " device(s):");
        deviceListContainer.removeAllViews();

        for (BluetoothDevice device : pairedDevices) {
            Button deviceButton = new Button(this);
            deviceButton.setText(device.getName() + "\n" + device.getAddress());
            deviceButton.setPadding(16, 16, 16, 16);
            deviceButton.setBackgroundColor(0xFF404040);
            deviceButton.setTextColor(0xFFFFFFFF);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 8, 8, 8);
            deviceButton.setLayoutParams(params);

            final BluetoothDevice finalDevice = device;
            deviceButton.setOnClickListener(v -> connectTo(finalDevice));

            deviceListContainer.addView(deviceButton);
        }
    }

    private void connectTo(BluetoothDevice device) {
        if (hidService == null) {
            statusText.setText("Error: HID Service not available");
            return;
        }

        statusText.setText("Connecting to: " + device.getName() + "...");
        Log.d(TAG, "Selected device: " + device.getName());

        hidService.connectToDevice(device, new BluetoothHIDService.ConnectionCallback() {
            @Override
            public void onConnected(BluetoothDevice device) {
                runOnUiThread(() -> {
                    statusText.setText("Connected to: " + device.getName());
                    Intent i = new Intent(
                            BluetoothConnectionActivity.this,
                            ModeSelectActivity.class
                    );
                    i.putExtra("deviceName", device.getName());
                    startActivity(i);
                    finish();
                });
            }

            @Override
            public void onError(@Nullable BluetoothDevice device, String message) {
                runOnUiThread(() -> {
                    String name = (device != null) ? device.getName() : "device";
                    statusText.setText("Failed to connect to " + name + ": " + message);
                    Log.w(TAG, "Connection error for " + name + ": " + message);
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
    }
}
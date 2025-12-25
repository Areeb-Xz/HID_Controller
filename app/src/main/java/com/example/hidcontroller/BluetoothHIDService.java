package com.example.hidcontroller;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import java.util.Set;

public class BluetoothHIDService extends Service {
    private static final String TAG = "BluetoothHIDService";
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice connectedDevice;
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        BluetoothHIDService getService() {
            return BluetoothHIDService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "BluetoothHIDService created");
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "BluetoothHIDService started");
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public BluetoothDevice[] getPairedDevices() {
        if (bluetoothAdapter == null) {
            return new BluetoothDevice[0];
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        return pairedDevices.toArray(new BluetoothDevice[0]);
    }

    public void connectToDevice(BluetoothDevice device) {
        Log.d(TAG, "Connecting to device: " + device.getName());
        connectedDevice = device;
    }

    public void sendKeyPress(String keyLabel) {
        sendKeyPress(new String[]{keyLabel});
    }

    public void sendKeyPress(String[] keys) {
        if (connectedDevice == null) {
            Log.w(TAG, "No device connected");
            return;
        }

        byte modifier = 0x00;
        byte[] keyCodes = new byte[6];
        int keyCount = 0;

        for (String keyLabel : keys) {
            int hidCode = HIDKeyCode.getHIDCode(keyLabel);
            if (hidCode == 0x00) continue;

            if (keyLabel.equals("Ctrl")) modifier |= 0x01;
            else if (keyLabel.equals("Shift")) modifier |= 0x02;
            else if (keyLabel.equals("Alt")) modifier |= 0x04;
            else if (keyLabel.equals("GUI")) modifier |= 0x08;
            else if (keyCount < 6) {
                keyCodes[keyCount++] = (byte) hidCode;
            }
        }

        if (keyCount == 0) {
            Log.w(TAG, "No valid keys");
            return;
        }

        // Key down
        sendHIDReport(modifier, keyCodes, keyCount);
        try { Thread.sleep(50); } catch (InterruptedException e) { }

        // Key up
        byte[] empty = new byte[8];
        Log.d(TAG, "Key UP: " + bytesToHex(empty));

        Log.d(TAG, "Combo sent: " + String.join(" + ", keys));
    }

    private void sendHIDReport(byte modifier, byte[] keyCodes, int keyCount) {
        byte[] report = new byte[8];
        report[0] = modifier;
        report[1] = 0x00;
        for (int i = 0; i < keyCount && i < 6; i++) {
            report[2 + i] = keyCodes[i];
        }
        Log.d(TAG, "HID Report: " + bytesToHex(report));
    }

    private void createAndSendReport(byte keyCode, byte modifier) {
        byte[] report = new byte[8];
        report[0] = modifier;  // modifier byte (Shift/Ctrl/etc.)
        report[1] = 0x00;      // reserved
        report[2] = keyCode;   // first key code
        report[3] = 0x00;
        report[4] = 0x00;
        report[5] = 0x00;
        report[6] = 0x00;
        report[7] = 0x00;

        Log.d(TAG, "HID Report created: " + bytesToHex(report));
    }

    public BluetoothDevice getConnectedDevice() {
        return connectedDevice;
    }

    public boolean isConnected() {
        return connectedDevice != null;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "BluetoothHIDService destroyed");
    }
}
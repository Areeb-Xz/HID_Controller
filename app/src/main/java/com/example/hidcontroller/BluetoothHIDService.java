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
        if (connectedDevice == null) {
            Log.w(TAG, "No device connected");
            return;
        }

        int hidCode = HIDKeyCode.getHIDCode(keyLabel);
        if (hidCode == 0x00) {
            Log.w(TAG, "Unknown key: " + keyLabel);
            return;
        }

        // Send key down
        sendHIDReport((byte) hidCode, (byte) 0x00);

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Send key up
        sendHIDReport((byte) 0x00, (byte) 0x00);

        Log.d(TAG, "Key sent: " + keyLabel + " (code: 0x" + String.format("%02X", hidCode) + ")");
    }

    private void sendHIDReport(byte keyCode, byte modifier) {
        byte[] report = new byte[8];
        report[0] = modifier;
        report[1] = 0x00;
        report[2] = keyCode;
        report[3] = 0x00;
        report[4] = 0x00;
        report[5] = 0x00;
        report[6] = 0x00;
        report[7] = 0x00;

        Log.d(TAG, "HID Report: " + bytesToHex(report));
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
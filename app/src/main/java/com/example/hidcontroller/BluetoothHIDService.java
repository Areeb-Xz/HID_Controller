package com.example.hidcontroller;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothProfile;
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
    private BluetoothHidDevice hidDevice;
    private boolean isConnected = false;
    private ConnectionCallback currentCallback;
    private final IBinder binder = new LocalBinder();

    // HID Profile Listener
    private BluetoothProfile.ServiceListener hidProfileListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = (BluetoothHidDevice) proxy;
                Log.d(TAG, "HID Device profile connected");
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = null;
                Log.d(TAG, "HID Device profile disconnected");
            }
        }
    };

    // Connection callback interface
    public interface ConnectionCallback {
        void onConnected(BluetoothDevice device);
        void onError(BluetoothDevice device, String message);
    }

    // Local binder
    public class LocalBinder extends Binder {
        public BluetoothHIDService getService() {
            return BluetoothHIDService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter != null) {
            bluetoothAdapter.getProfileProxy(this, hidProfileListener, BluetoothProfile.HID_DEVICE);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
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

    public void connectToDevice(BluetoothDevice device, ConnectionCallback callback) {
        Log.d(TAG, "Attempting to connect to device: " + device.getName());
        currentCallback = callback;

        if (device == null) {
            if (callback != null) {
                callback.onError(null, "Device is null");
            }
            return;
        }

        connectedDevice = device;

        // TODO: Implement real HID host connection logic here
        // For now, simulate successful connection
        new Thread(() -> {
            try {
                Thread.sleep(1000); // Simulate connection delay
                isConnected = true;

                if (currentCallback != null) {
                    currentCallback.onConnected(device);
                    Log.d(TAG, "Connected to: " + device.getName());
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Connection thread interrupted", e);
                if (currentCallback != null) {
                    currentCallback.onError(device, "Connection interrupted");
                }
            }
        }).start();
    }

    public boolean isConnected() {
        return isConnected;
    }

    public BluetoothDevice getConnectedDevice() {
        return connectedDevice;
    }

    public void disconnect() {
        Log.d(TAG, "Disconnecting from device");
        isConnected = false;
        connectedDevice = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
        if (bluetoothAdapter != null) {
            bluetoothAdapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice);
        }
    }
}
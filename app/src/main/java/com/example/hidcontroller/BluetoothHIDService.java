package com.example.hidcontroller;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothHidDevice;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.Set;

public class BluetoothHIDService extends Service {

    private static final String TAG = "BluetoothHIDService";

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothHidDevice hidDevice;
    private BluetoothDevice connectedDevice;
    private boolean isConnected = false;

    public interface ConnectionCallback {
        void onConnected(BluetoothDevice device);
        void onError(@Nullable BluetoothDevice device, String message);
    }

    private ConnectionCallback currentCallback;

    // ----- Profile listener (JUST set hidDevice) -----
    private final BluetoothProfile.ServiceListener hidProfileListener =
            new BluetoothProfile.ServiceListener() {
                @Override
                public void onServiceConnected(int profile, BluetoothProfile proxy) {
                    if (profile == BluetoothProfile.HID_DEVICE) {
                        hidDevice = (BluetoothHidDevice) proxy;
                        Log.d(TAG, "HID Device profile connected");
                        // NO registerApp yet – connection only
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

    // ----- Binder -----
    public class LocalBinder extends Binder {
        public BluetoothHIDService getService() {
            return BluetoothHIDService.this;
        }
    }

    private final IBinder binder = new LocalBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            bluetoothAdapter.getProfileProxy(
                    this,
                    hidProfileListener,
                    BluetoothProfile.HID_DEVICE
            );
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

    // ----- Paired devices -----
    public BluetoothDevice[] getPairedDevices() {
        if (bluetoothAdapter == null) return new BluetoothDevice[0];
        Set<BluetoothDevice> paired = bluetoothAdapter.getBondedDevices();
        return paired.toArray(new BluetoothDevice[0]);
    }

    // ----- Connect API -----
    public void connectToDevice(@Nullable BluetoothDevice device, @Nullable ConnectionCallback callback) {
        Log.d(TAG, "Attempting to connect to device: "
                + (device != null ? device.getName() : "null"));
        currentCallback = callback;

        if (device == null) {
            if (callback != null) callback.onError(null, "Device is null");
            return;
        }

        connectedDevice = device;

        if (hidDevice == null) {
            Log.e(TAG, "HID profile not ready when trying to connect");
            if (currentCallback != null) currentCallback.onError(device, "HID profile not ready");
            return;
        }
        boolean started = hidDevice.connect(device);
        Log.d(TAG, "hidDevice.connect() started=" + started);
    }

    // ----- HID connection state callback -----
    private final BluetoothHidDevice.Callback hidCallback =
            new BluetoothHidDevice.Callback() {
                @Override
                public void onConnectionStateChanged(BluetoothDevice device, int state) {
                    if (connectedDevice == null || !device.equals(connectedDevice)) return;

                    if (state == BluetoothProfile.STATE_CONNECTED) {
                        isConnected = true;
                        Log.d(TAG, "HID connected to " + device.getName());
                        if (currentCallback != null) currentCallback.onConnected(device);
                    } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                        isConnected = false;
                        Log.d(TAG, "HID disconnected from " + device.getName());
                        if (currentCallback != null) {
                            currentCallback.onError(device, "Disconnected");
                        }
                    }
                }
            };

    public boolean isConnected() {
        return isConnected;
    }

    @Nullable
    public BluetoothDevice getConnectedDevice() {
        return connectedDevice;
    }

    public void disconnect() {
        Log.d(TAG, "Disconnecting from device");
        if (hidDevice != null && connectedDevice != null) {
            hidDevice.disconnect(connectedDevice);
        }
        isConnected = false;
        connectedDevice = null;
    }

    // ----- Mouse APIs -----
    public void sendMouseMovement(int deltaX, int deltaY) {
        if (!isConnected || connectedDevice == null || hidDevice == null) {
            Log.w(TAG, "sendMouseMovement: not connected");
            return;
        }
        byte buttons = 0x00;
        byte dx = (byte) deltaX;
        byte dy = (byte) deltaY;
        byte wheel = 0x00;
        byte[] report = new byte[]{buttons, dx, dy, wheel};
        Log.d(TAG, "sendMouseMovement report: " +
                java.util.Arrays.toString(report));
        hidDevice.sendReport(connectedDevice, 1, report);
    }

    public void sendMouseClick(int button) {
        if (!isConnected || connectedDevice == null || hidDevice == null) {
            Log.w(TAG, "sendMouseClick: not connected");
            return;
        }
        byte buttons = (byte) (1 << (button - 1));
        byte dx = 0x00;
        byte dy = 0x00;
        byte wheel = 0x00;
        byte[] report = new byte[]{buttons, dx, dy, wheel};
        Log.d(TAG, "sendMouseClick report: " +
                java.util.Arrays.toString(report));
        hidDevice.sendReport(connectedDevice, 1, report);
    }

    public void sendMouseScroll(int scrollAmount) {
        if (!isConnected || connectedDevice == null || hidDevice == null) {
            Log.w(TAG, "sendMouseScroll: not connected");
            return;
        }
        byte buttons = 0x00;
        byte dx = 0x00;
        byte dy = 0x00;
        byte wheel = (byte) scrollAmount;
        byte[] report = new byte[]{buttons, dx, dy, wheel};
        Log.d(TAG, "sendMouseScroll report: " +
                java.util.Arrays.toString(report));
        hidDevice.sendReport(connectedDevice, 1, report);
    }

    public void sendKeyPress(int keyCode) { /* TODO: implement later */ }
    public void sendKeyPress(String key) { /* TODO */ }
    public void sendKeyPress(String[] keys) { /* TODO */ }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
        if (bluetoothAdapter != null && hidDevice != null) {
            bluetoothAdapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice);
        }
    }
}
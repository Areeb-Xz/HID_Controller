package com.example.hidcontroller;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothHidDeviceAppSdpSettings;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.Set;
import java.util.concurrent.Executor;


public class BluetoothHIDService extends Service {

    private static final String TAG = "BluetoothHIDService";
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothHidDevice hidDevice;
    private BluetoothDevice connectedDevice;
    private boolean isConnected = false;
    private boolean isHIDRegistered = false;
    private Handler handler = new Handler(Looper.getMainLooper());
    private static final byte MOUSE_BTN_LEFT = 0x01;

    public interface ConnectionCallback {
        void onConnected(BluetoothDevice device);
        void onError(@Nullable BluetoothDevice device, String message);
    }

    private ConnectionCallback currentCallback;

    // ----- Profile listener -----
    private final BluetoothProfile.ServiceListener hidProfileListener =
            new BluetoothProfile.ServiceListener() {
                @Override
                public void onServiceConnected(int profile, BluetoothProfile proxy) {
                    if (profile == BluetoothProfile.HID_DEVICE) {
                        hidDevice = (BluetoothHidDevice) proxy;
                        Log.d(TAG, "HID Device profile connected");
                        registerHIDApp();
                    }
                }

                @Override
                public void onServiceDisconnected(int profile) {
                    if (profile == BluetoothProfile.HID_DEVICE) {
                        hidDevice = null;
                        isHIDRegistered = false;
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

    // ----- Register HID App -----
    private void registerHIDApp() {
        if (hidDevice == null) {
            Log.e(TAG, "Cannot register: hidDevice is null");
            return;
        }

        if (isHIDRegistered) {
            Log.d(TAG, "HID app already registered");
            return;
        }

        try {
            byte[] descriptor = getKeyboardMouseDescriptor();

            BluetoothHidDeviceAppSdpSettings sdp = new BluetoothHidDeviceAppSdpSettings(
                    "HID Controller",
                    "Android HID Keyboard/Mouse",
                    "HID Controller",
                    (byte) 0x04,  // SUBCLASS_COMBO
                    descriptor
            );

            Executor executor = ContextCompat.getMainExecutor(this);

            boolean registered = hidDevice.registerApp(sdp, null, null, executor, hidCallback);
            Log.d(TAG, "registerApp() returned: " + registered);
            isHIDRegistered = registered;

            if (!registered) {
                Log.e(TAG, "Failed to register HID app");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error registering HID app: " + e.getMessage(), e);
        }
    }

    /**
     * USB HID Descriptor for Keyboard + Mouse
     * Report ID 1: Keyboard [Modifiers, Reserved, KeyCode1-6] = 8 bytes
     * Report ID 2: Mouse [Buttons, X, Y, Wheel] = 5 bytes
     */
    private byte[] getKeyboardMouseDescriptor() {
        return new byte[]{
                // ===== Keyboard (Report ID 1) =====
                (byte) 0x05, (byte) 0x01,        // Usage Page (Generic Desktop)
                (byte) 0x09, (byte) 0x06,        // Usage (Keyboard)
                (byte) 0xa1, (byte) 0x01,        // Collection (Application)
                (byte) 0x85, (byte) 0x01,        // Report ID (1)
                (byte) 0x05, (byte) 0x07,        // Usage Page (Keyboard/Keypad)
                (byte) 0x19, (byte) 0xe0,        // Usage Minimum (Keyboard Left Control)
                (byte) 0x29, (byte) 0xe7,        // Usage Maximum (Keyboard Right GUI)
                (byte) 0x15, (byte) 0x00,        // Logical Minimum (0)
                (byte) 0x25, (byte) 0x01,        // Logical Maximum (1)
                (byte) 0x75, (byte) 0x01,        // Report Size (1 bit)
                (byte) 0x95, (byte) 0x08,        // Report Count (8 = modifiers)
                (byte) 0x81, (byte) 0x02,        // Input (Data, Variable, Absolute)
                (byte) 0x95, (byte) 0x01,        // Report Count (1)
                (byte) 0x75, (byte) 0x08,        // Report Size (8 bits)
                (byte) 0x81, (byte) 0x01,        // Input (Constant) = reserved
                (byte) 0x95, (byte) 0x06,        // Report Count (6 keys)
                (byte) 0x75, (byte) 0x08,        // Report Size (8 bits each)
                (byte) 0x15, (byte) 0x00,        // Logical Minimum (0)
                (byte) 0x25, (byte) 0x65,        // Logical Maximum (101)
                (byte) 0x05, (byte) 0x07,        // Usage Page (Keyboard/Keypad)
                (byte) 0x19, (byte) 0x00,        // Usage Minimum
                (byte) 0x29, (byte) 0x65,        // Usage Maximum
                (byte) 0x81, (byte) 0x00,        // Input (Data, Array, Absolute)
                (byte) 0xc0,                     // End Collection (Keyboard)

                // ===== Mouse (Report ID 2) =====
                (byte) 0x05, (byte) 0x01,        // Usage Page (Generic Desktop)
                (byte) 0x09, (byte) 0x02,        // Usage (Mouse)
                (byte) 0xa1, (byte) 0x01,        // Collection (Application)
                (byte) 0x85, (byte) 0x02,        // Report ID (2)
                (byte) 0x09, (byte) 0x01,        // Usage (Pointer)
                (byte) 0xa1, (byte) 0x00,        // Collection (Physical)
                (byte) 0x05, (byte) 0x09,        // Usage Page (Button)
                (byte) 0x19, (byte) 0x01,        // Usage Minimum (Button 1)
                (byte) 0x29, (byte) 0x03,        // Usage Maximum (Button 3)
                (byte) 0x15, (byte) 0x00,        // Logical Minimum (0)
                (byte) 0x25, (byte) 0x01,        // Logical Maximum (1)
                (byte) 0x75, (byte) 0x01,        // Report Size (1 bit each)
                (byte) 0x95, (byte) 0x03,        // Report Count (3 buttons)
                (byte) 0x81, (byte) 0x02,        // Input (Data, Variable, Absolute)
                (byte) 0x95, (byte) 0x01,        // Report Count (1)
                (byte) 0x75, (byte) 0x05,        // Report Size (5 bits padding)
                (byte) 0x81, (byte) 0x01,        // Input (Constant)
                (byte) 0x05, (byte) 0x01,        // Usage Page (Generic Desktop)
                (byte) 0x09, (byte) 0x30,        // Usage (X)
                (byte) 0x09, (byte) 0x31,        // Usage (Y)
                (byte) 0x09, (byte) 0x38,        // Usage (Wheel)
                (byte) 0x15, (byte) 0x81,        // Logical Minimum (-127)
                (byte) 0x25, (byte) 0x7f,        // Logical Maximum (127)
                (byte) 0x75, (byte) 0x08,        // Report Size (8 bits each)
                (byte) 0x95, (byte) 0x03,        // Report Count (3 = X, Y, Wheel)
                (byte) 0x81, (byte) 0x06,        // Input (Data, Variable, Relative)
                (byte) 0xc0,                     // End Collection (Physical)
                (byte) 0xc0                      // End Collection (Mouse)
        };
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

        if (!isHIDRegistered) {
            Log.e(TAG, "HID app not registered yet");
            if (currentCallback != null) currentCallback.onError(device, "HID not registered");
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

    // ===== KEYBOARD METHODS =====
    /**
     * Send a single key press (e.g., "A", "Enter", "F1")
     * Sends PRESS report, waits 10ms, then RELEASE report
     */
    public void sendKeyPress(String key) {
        if (!isConnected || connectedDevice == null || hidDevice == null) {
            Log.w(TAG, "sendKeyPress: not connected");
            return;
        }

        int hidCode = HIDKeyCode.getHIDCode(key);
        if (hidCode == 0x00) {
            Log.w(TAG, "sendKeyPress: unknown key: " + key);
            return;
        }

        // Build PRESS report: [modifiers(0), reserved, keyCode, 0, 0, 0, 0, 0]
        byte[] reportPress = new byte[8];
        reportPress[0] = 0x00;  // no modifiers
        reportPress[1] = 0x00;  // reserved
        reportPress[2] = (byte) hidCode;

        Log.d(TAG, "sendKeyPress '" + key + "' code=0x" + String.format("%02X", hidCode) + " PRESS");
        hidDevice.sendReport(connectedDevice, 1, reportPress);

        // Use Handler.postDelayed() instead of Thread.sleep() - doesn't block UI!
        handler.postDelayed(() -> {
            if (isConnected && connectedDevice != null && hidDevice != null) {
                // RELEASE report (all zeros)
                byte[] reportRelease = new byte[8];
                Log.d(TAG, "sendKeyPress '" + key + "' RELEASE");
                hidDevice.sendReport(connectedDevice, 1, reportRelease);
            }
        }, 10);  // 10ms delay
    }

    /**
     * Send a key combo like Shift+A or Ctrl+S
     * Example: sendKeyPress(new String[]{"Shift", "A"})
     * FIXED: Now sends PRESS + RELEASE reports with proper timing using Handler
     */
    public void sendKeyPress(String[] keys) {
        if (!isConnected || connectedDevice == null || hidDevice == null) {
            Log.w(TAG, "sendKeyPress: not connected");
            return;
        }

        if (keys == null || keys.length == 0) {
            Log.w(TAG, "sendKeyPress: empty key array");
            return;
        }

        // Extract modifiers and the main key
        byte modifiers = 0x00;
        int mainKeyCode = 0x00;
        for (String key : keys) {
            switch (key) {
                case "Ctrl":
                    modifiers |= 0x01;  // LEFT_CTRL
                    break;
                case "Shift":
                    modifiers |= 0x02;  // LEFT_SHIFT
                    break;
                case "Alt":
                    modifiers |= 0x04;  // LEFT_ALT
                    break;
                case "GUI":
                    modifiers |= 0x08;  // LEFT_GUI
                    break;
                default:
                    mainKeyCode = HIDKeyCode.getHIDCode(key);
                    break;
            }
        }

        if (mainKeyCode == 0x00) {
            Log.w(TAG, "sendKeyPress: could not find main key in: " + java.util.Arrays.toString(keys));
            return;
        }

        // Build PRESS report: [modifiers, reserved, keyCode1-6]
        byte[] reportPress = new byte[8];
        reportPress[0] = modifiers;
        reportPress[1] = 0x00;  // reserved
        reportPress[2] = (byte) mainKeyCode;

        Log.d(TAG, "sendKeyPress combo " + java.util.Arrays.toString(keys)
                + " modifiers=0x" + String.format("%02X", modifiers)
                + " keyCode=0x" + String.format("%02X", mainKeyCode) + " PRESS");

        // SEND PRESS REPORT
        hidDevice.sendReport(connectedDevice, 1, reportPress);

        // Use Handler.postDelayed() instead of Thread.sleep()
        handler.postDelayed(() -> {
            if (isConnected && connectedDevice != null && hidDevice != null) {
                // RELEASE report (all zeros) - THIS IS THE CRITICAL FIX!
                byte[] reportRelease = new byte[8];
                Log.d(TAG, "sendKeyPress combo RELEASE");
                hidDevice.sendReport(connectedDevice, 1, reportRelease);
            }
        }, 10);  // 10ms delay
    }

    /**
     * Send by HID code (for advanced use)
     */
    public void sendKeyPress(int keyCode) {
        if (!isConnected || connectedDevice == null || hidDevice == null) {
            Log.w(TAG, "sendKeyPress: not connected");
            return;
        }

        // PRESS
        byte[] reportPress = new byte[8];
        reportPress[0] = 0x00;  // no modifiers
        reportPress[1] = 0x00;  // reserved
        reportPress[2] = (byte) keyCode;

        Log.d(TAG, "sendKeyPress 0x" + String.format("%02X", keyCode) + " PRESS");
        hidDevice.sendReport(connectedDevice, 1, reportPress);

        // RELEASE with Handler
        handler.postDelayed(() -> {
            if (isConnected && connectedDevice != null && hidDevice != null) {
                byte[] reportRelease = new byte[8];
                Log.d(TAG, "sendKeyPress 0x" + String.format("%02X", keyCode) + " RELEASE");
                hidDevice.sendReport(connectedDevice, 1, reportRelease);
            }
        }, 10);
    }

    // ===== MOUSE METHODS =====
    /**
     * Send mouse movement (deltaX, deltaY in range [-127, 127])
     */
    public void sendMouseMovement(int deltaX, int deltaY) {
        if (!isConnected || connectedDevice == null || hidDevice == null) {
            Log.w(TAG, "sendMouseMovement: not connected");
            return;
        }

        byte dx = (byte) Math.max(-127, Math.min(127, deltaX));
        byte dy = (byte) Math.max(-127, Math.min(127, deltaY));

        byte[] report = new byte[5];
        report[0] = 0x00;  // Buttons
        report[1] = dx;    // X
        report[2] = dy;    // Y
        report[3] = 0x00;  // Wheel
        report[4] = 0x00;  // Padding

        boolean ok = hidDevice.sendReport(connectedDevice, 2, report);
        Log.d(TAG, "sendMouseMovement ok=" + ok + " dx=" + dx + " dy=" + dy + " len=" + report.length);
    }

    // Click: [Buttons, 0, 0, 0, Padding]
    public void sendMouseClick(int button) {
        if (!isConnected || connectedDevice == null || hidDevice == null) {
            Log.w(TAG, "sendMouseClick: not connected");
            return;
        }

        byte buttonByte = (byte) (button == 1 ? 0x01 : button == 2 ? 0x02 : button == 3 ? 0x04 : 0x00);

        byte[] press = new byte[5];
        press[0] = buttonByte; // Buttons
        press[1] = 0x00;
        press[2] = 0x00;
        press[3] = 0x00;
        press[4] = 0x00;       // Padding

        boolean ok = hidDevice.sendReport(connectedDevice, 2, press);
        Log.d(TAG, "sendMouseClick PRESS ok=" + ok + " button=0x" + String.format("%02X", buttonByte));

        handler.postDelayed(() -> {
            if (isConnected && connectedDevice != null && hidDevice != null) {
                byte[] release = new byte[5];
                release[0] = 0x00; // Buttons up
                release[1] = 0x00;
                release[2] = 0x00;
                release[3] = 0x00;
                release[4] = 0x00; // Padding
                hidDevice.sendReport(connectedDevice, 2, release);
                Log.d(TAG, "sendMouseClick RELEASE");
            }
        }, 10);
    }

    // Scroll: [0, 0, 0, Wheel, Padding]
    public void sendMouseScroll(int scrollAmount) {
        if (!isConnected || connectedDevice == null || hidDevice == null) {
            Log.w(TAG, "sendMouseScroll: not connected");
            return;
        }

        byte wheel = (byte) Math.max(-127, Math.min(127, scrollAmount));

        byte[] report = new byte[5];
        report[0] = 0x00;  // Buttons
        report[1] = 0x00;  // X
        report[2] = 0x00;  // Y
        report[3] = wheel; // Wheel
        report[4] = 0x00;  // Padding

        boolean ok = hidDevice.sendReport(connectedDevice, 2, report);
        Log.d(TAG, "sendMouseScroll ok=" + ok + " wheel=" + wheel + " len=" + report.length);
    }

    public void sendMouseReport(byte buttons, int dxInt, int dyInt, int wheelInt) {
        if (!isConnected || connectedDevice == null || hidDevice == null) {
            Log.w(TAG, "sendMouseReport: not connected");
            return;
        }

        byte dx = (byte) Math.max(-127, Math.min(127, dxInt));
        byte dy = (byte) Math.max(-127, Math.min(127, dyInt));
        byte wheel = (byte) Math.max(-127, Math.min(127, wheelInt));

        // 5 bytes (Windows-friendly in your setup): [Buttons, X, Y, Wheel, Padding]
        byte[] report = new byte[5];
        report[0] = buttons;
        report[1] = dx;
        report[2] = dy;
        report[3] = wheel;
        report[4] = 0x00;

        hidDevice.sendReport(connectedDevice, 2, report);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
        if (bluetoothAdapter != null && hidDevice != null) {
            bluetoothAdapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice);
        }
    }
}
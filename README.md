# Android Bluetooth HID Controller

An educational Android app that turns your phone into a **virtual Bluetooth HID keyboard**.  
It walks through building a custom on‑screen keyboard, mapping keys to HID scan codes, and structuring a Bluetooth HID service for sending key reports. [conversation_history:1]

## Features

- Home screen with simple navigation (Keyboard, Settings). [conversation_history:1]  
- Virtual 5×10 QWERTY keyboard with visual press feedback. [conversation_history:1]  
- Status bar showing current key events and state. [conversation_history:1]  
- Bluetooth "Settings" screen listing already‑paired devices. [conversation_history:1]  
- Service that builds HID keyboard reports (modifier + key codes) for each press. [conversation_history:1]  
- Shift key implemented as a one‑shot modifier (Shift + next key). [conversation_history:1]

> Note: In this learning version, HID reports are constructed and logged; actually acting as a full system Bluetooth keyboard can require extra HID profile support and system‑level privileges. [conversation_history:1]

## Project Structure

- `MainActivity` – Home screen, buttons to keyboard and Bluetooth settings. [file:47]  
- `KeyboardActivity` – Renders the key grid, handles touch events, calls HID service on key up. [file:45]  
- `BluetoothConnectionActivity` – Shows paired devices and lets you choose one to "connect". [file:93]  
- `BluetoothHIDService` – Manages Bluetooth adapter, selected device, and HID report creation. [file:93]  
- `HIDKeyCode` – Maps labels like `Q`, `1`, `Enter`, `Space` to USB HID scan codes (0x04–0x38, etc.). [conversation_history:1]  
- `activity_keyboard.xml` – Layout with status bar and 5×10 `GridLayout` for keys. [file:45]  
- `activity_bluetooth_connection.xml` – Simple list layout for paired devices. [conversation_history:1]

## How It Works

1. User launches app → sees `MainActivity`. [file:93]  
2. Tapping **Keyboard** opens `KeyboardActivity` and shows a 50‑key grid. [file:45]  
3. Tapping **Settings** opens `BluetoothConnectionActivity`, listing paired devices. [file:93]  
4. Selecting a device sets it as the current target in `BluetoothHIDService` and returns to the keyboard. [file:93]  
5. Pressing a key:  
   - `KeyboardActivity` logs `Key pressed: X` and updates the status bar. [file:45]  
   - `HIDKeyCode` turns the label into a HID scan code (for `Q`, 0x14). [conversation_history:1]  
   - `BluetoothHIDService` builds an 8‑byte HID keyboard report:  
     - Byte 0: modifier (Shift, Ctrl, etc.)  
     - Byte 1: reserved  
     - Bytes 2–7: up to 6 simultaneous key codes [conversation_history:1]  
   - Two reports are created: key‑down (code set) and key‑up (all zeros). [conversation_history:1]  
   - Reports are logged to Logcat for inspection. [conversation_history:1]

## Setup & Requirements

- Android Studio (Giraffe or newer recommended). [web:72]  
- Android device with Bluetooth support.  
- Minimum SDK: use your project's `minSdkVersion`.  
- Ensure Bluetooth permissions in `AndroidManifest.xml`. [conversation_history:1]

## Running the App

1. Clone/open in Android Studio.  
2. Build and run on physical device. [web:72]  
3. Pair target device via Android settings.  
4. App: **Settings** → pick device → **Keyboard** → press keys, check Logcat. [conversation_history:1]

## Educational Goals

- Custom `GridLayout` keyboard UI. [file:45]  
- HID scan code mapping. [conversation_history:1]  
- Service binding patterns. [file:93]  
- HID report format + modifiers. [conversation_history:1]

## Next Steps

- Full Bluetooth HID profile.  
- Multi-key combos (Ctrl+Alt).  
- Visual Shift state (letter case). [conversation_history:1]

---
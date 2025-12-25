# Android Bluetooth HID Controller

An educational Android app that turns your phone into a **virtual Bluetooth HID keyboard**.  
It walks through building a custom on‑screen keyboard, mapping keys to HID scan codes, and structuring a Bluetooth HID service for sending key reports.


## Features

- Home screen with simple navigation (Keyboard, Settings).
- Virtual 5×10 QWERTY keyboard with visual press feedback.
- Status bar showing current key events and state.
- Bluetooth "Settings" screen listing already‑paired devices.
- Service that builds HID keyboard reports (modifier + key codes) for each press.
- Shift key implemented as a one‑shot modifier (Shift + next key).

> Note: In this learning version, HID reports are constructed and logged; actually acting as a full system Bluetooth keyboard can require extra HID profile support and system‑level privileges.


## Project Structure

- `MainActivity` – Home screen, buttons to keyboard and Bluetooth settings.
- `KeyboardActivity` – Renders the key grid, handles touch events, calls HID service on key up.
- `BluetoothConnectionActivity` – Shows paired devices and lets you choose one to "connect".
- `BluetoothHIDService` – Manages Bluetooth adapter, selected device, and HID report creation.
- `HIDKeyCode` – Maps labels like `Q`, `1`, `Enter`, `Space` to USB HID scan codes (0x04–0x38, etc.).
  
- `activity_keyboard.xml` – Layout with status bar and 5×10 `GridLayout` for keys.
- `activity_bluetooth_connection.xml` – Simple list layout for paired devices.


## How It Works

1. User launches app → sees `MainActivity`.
2. Tapping **Keyboard** opens `KeyboardActivity` and shows a 50‑key grid.
3. Tapping **Settings** opens `BluetoothConnectionActivity`, listing paired devices.
4. Selecting a device sets it as the current target in `BluetoothHIDService` and returns to the keyboard.
5. Pressing a key:  
   - `KeyboardActivity` logs `Key pressed: X` and updates the status bar.
   - `HIDKeyCode` turns the label into a HID scan code (for `Q`, 0x14).
     
   - `BluetoothHIDService` builds an 8‑byte HID keyboard report:
     - Byte 0: modifier (Shift, Ctrl, etc.)
     - Byte 1: reserved
     - Bytes 2–7: up to 6 simultaneous key codes
       
   - Two reports are created: key‑down (code set) and key‑up (all zeros).
     
   - Reports are logged to Logcat for inspection.
   

## Setup & Requirements

- Android Studio (Giraffe or newer recommended).
- Android device with Bluetooth support.
- Minimum SDK: use your project's `minSdkVersion`.
- Ensure Bluetooth permissions in `AndroidManifest.xml`.


## Running the App

1. Clone/open in Android Studio.
2. Build and run on physical device.
3. Pair target device via Android settings.
4. App: **Settings** → pick device → **Keyboard** → press keys, check Logcat.


## Educational Goals

- Custom `GridLayout` keyboard UI.
- HID scan code mapping.
  
- Service binding patterns.
- HID report format + modifiers.


## Next Steps

- Full Bluetooth HID profile.
- Multi-key combos (Ctrl+Alt).
- Visual Shift state (letter case).
---
package com.example.hidcontroller;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.CheckBox;

import androidx.appcompat.app.AppCompatActivity;

public class KeyboardActivity extends AppCompatActivity {

    private static final String TAG = "KeyboardActivity";
    private TextView keyboardStatus;
    private BluetoothHIDService hidService;
    private boolean isBound = false;
    private boolean isFnActive = false;
    private static final int MAX_COMBOS = 14;
    private Button[] comboSlots = new Button[MAX_COMBOS];
    private String[][] comboKeys = new String[MAX_COMBOS][];
    private boolean isShiftLatched = false;
    private java.util.Map<Button, String> baseLabels = new java.util.HashMap<>();
    private static final String[] ALL_MAIN_KEYS = new String[] {
            "Esc", "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12",
            "`", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "=",
            "Tab", "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "[", "]", "\\",
            "Caps", "A", "S", "D", "F", "G", "H", "J", "K", "L", ";", "'", "Enter",
            "Shift", "Z", "X", "C", "V", "B", "N", "M", ",", ".", "/", "Space",
            "Ctrl", "Alt", "GUI", "Menu", "Back"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keyboard);
        keyboardStatus = findViewById(R.id.keyboardStatus);
        bindAllKeys();
        bindComboRow();
        loadCombos();
        logToStatus("Keyboard initialized");
        Intent serviceIntent = new Intent(this, BluetoothHIDService.class);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    // ---------------- Keyboard binding ----------------
    private void bindAllKeys() {
        // Function row
        bindKey(R.id.keyEsc, "Esc");
        bindKey(R.id.keyF1, "F1");
        bindKey(R.id.keyF2, "F2");
        bindKey(R.id.keyF3, "F3");
        bindKey(R.id.keyF4, "F4");
        bindKey(R.id.keyF5, "F5");
        bindKey(R.id.keyF6, "F6");
        bindKey(R.id.keyF7, "F7");
        bindKey(R.id.keyF8, "F8");
        bindKey(R.id.keyF9, "F9");
        bindKey(R.id.keyF10, "F10");
        bindKey(R.id.keyF11, "F11");
        bindKey(R.id.keyF12, "F12");
        // Number row
        bindKey(R.id.keyBacktick, "`");
        bindKey(R.id.key1, "1");
        bindKey(R.id.key2, "2");
        bindKey(R.id.key3, "3");
        bindKey(R.id.key4, "4");
        bindKey(R.id.key5, "5");
        bindKey(R.id.key6, "6");
        bindKey(R.id.key7, "7");
        bindKey(R.id.key8, "8");
        bindKey(R.id.key9, "9");
        bindKey(R.id.key0, "0");
        bindKey(R.id.keyMinus, "-");
        bindKey(R.id.keyEquals, "=");
        bindKey(R.id.keyBackspace, "Back");
        // Q row
        bindKey(R.id.keyTab, "Tab");
        bindKey(R.id.keyQ, "Q");
        bindKey(R.id.keyW, "W");
        bindKey(R.id.keyE, "E");
        bindKey(R.id.keyR, "R");
        bindKey(R.id.keyT, "T");
        bindKey(R.id.keyY, "Y");
        bindKey(R.id.keyU, "U");
        bindKey(R.id.keyI, "I");
        bindKey(R.id.keyO, "O");
        bindKey(R.id.keyP, "P");
        bindKey(R.id.keyBracketLeft, "[");
        bindKey(R.id.keyBracketRight, "]");
        bindKey(R.id.keyBackslash, "\\");
        // A row
        bindKey(R.id.keyCaps, "Caps");
        bindKey(R.id.keyA, "A");
        bindKey(R.id.keyS, "S");
        bindKey(R.id.keyD, "D");
        bindKey(R.id.keyF, "F");
        bindKey(R.id.keyG, "G");
        bindKey(R.id.keyH, "H");
        bindKey(R.id.keyJ, "J");
        bindKey(R.id.keyK, "K");
        bindKey(R.id.keyL, "L");
        bindKey(R.id.keySemicolon, ";");
        bindKey(R.id.keyQuote, "'");
        bindKey(R.id.keyEnter, "Enter");
        // Z row
        bindShiftKey(R.id.keyShiftLeft);
        bindKey(R.id.keyZ, "Z");
        bindKey(R.id.keyX, "X");
        bindKey(R.id.keyC, "C");
        bindKey(R.id.keyV, "V");
        bindKey(R.id.keyB, "B");
        bindKey(R.id.keyN, "N");
        bindKey(R.id.keyM, "M");
        bindKey(R.id.keyComma, ",");
        bindKey(R.id.keyPeriod, ".");
        bindKey(R.id.keySlash, "/");
        bindShiftKey(R.id.keyShiftRight);
        // Bottom row
        bindKey(R.id.keyCtrlLeft, "Ctrl");
        bindFnKey(R.id.keyFn);           // Fn is special: no HID code
        bindKey(R.id.keyWin, "GUI");
        bindKey(R.id.keyAltLeft, "Alt");
        bindKey(R.id.keySpace, "Space");
        bindKey(R.id.keyAltRight, "Alt");
        bindKey(R.id.keyMenu, "Menu");
        bindKey(R.id.keyCtrlRight, "Ctrl");
    }

    private void bindKey(int viewId, String label) {
        Button b = findViewById(viewId);
        if (b == null) return;

        baseLabels.put(b, label);
        b.setOnTouchListener((v, event) -> {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                v.setBackgroundColor(0xFF606060);
                onKeyDown(label);
                return true;
            } else if (action == MotionEvent.ACTION_UP ||
                    action == MotionEvent.ACTION_CANCEL) {
                v.setBackgroundColor(0xFF404040);
                onKeyUp(label);
                v.performClick();
                return true;
            }
            return false;
        });
    }

    // Fn toggle only (no HID)
    private void bindFnKey(int viewId) {
        Button b = findViewById(viewId);
        if (b == null) return;

        b.setOnClickListener(v -> {
            isFnActive = !isFnActive;
            v.setBackgroundColor(isFnActive ? 0xFF008080 : 0xFF404040);
            logToStatus("Fn " + (isFnActive ? "ON" : "OFF"));
        });
    }

    private void onKeyDown(String label) {
        logToStatus("Key DOWN: " + label);
        Log.d(TAG, "Key DOWN: " + label);
    }

    private void onKeyUp(String label) {
        logToStatus("Key UP: " + label);
        Log.d(TAG, "onKeyUp label=" + label + " shiftLatched=" + isShiftLatched);
        if ("Fn".equals(label)) return;
        if (hidService == null || !hidService.isConnected()) {
            logToStatus("Not connected to device");
            return;
        }
        int code = HIDKeyCode.getHIDCode(label);
        if (code == 0x00) {
            Log.w(TAG, "Unknown HID label: " + label);
            return;
        }
        hidService.sendKeyPress(label);

        // force one‑shot shift
        if (isShiftLatched && !label.equals("Shift")) {
            Log.d(TAG, "Clearing Shift latch after key: " + label);
            Log.d(TAG, "Auto-clear shift after: " + label);
            isShiftLatched = false;
            updateShiftButtonsUi();
            refreshShiftSensitiveLabels();
        }
    }

    private void bindShiftKey(int viewId) {
        Button b = findViewById(viewId);
        if (b == null) return;
        baseLabels.put(b, "Shift");
        b.setOnClickListener(v -> {
            isShiftLatched = !isShiftLatched;
            updateShiftButtonsUi();
            refreshShiftSensitiveLabels();
        });
    }

    // ---------------- Combo row binding ----------------
    private void bindComboRow() {
        comboSlots[0] = findViewById(R.id.comboSlot1);
        comboSlots[1] = findViewById(R.id.comboSlot2);
        comboSlots[2] = findViewById(R.id.comboSlot3);
        comboSlots[3] = findViewById(R.id.comboSlot4);
        comboSlots[4] = findViewById(R.id.comboSlot5);
        comboSlots[5] = findViewById(R.id.comboSlot6);
        comboSlots[6] = findViewById(R.id.comboSlot7);
        comboSlots[7] = findViewById(R.id.comboSlot8);
        comboSlots[8] = findViewById(R.id.comboSlot9);
        comboSlots[9] = findViewById(R.id.comboSlot10);
        comboSlots[10] = findViewById(R.id.comboSlot11);
        comboSlots[11] = findViewById(R.id.comboSlot12);
        comboSlots[12] = findViewById(R.id.comboSlot13);
        comboSlots[13] = findViewById(R.id.comboSlot14);

        // Click handler for all slots
        for (int i = 0; i < MAX_COMBOS; i++) {
            final int index = i;
            Button slot = comboSlots[i];
            if (slot == null) continue;
            slot.setOnClickListener(v -> onComboSlotClicked(index));
            slot.setOnLongClickListener(v -> {
                clearComboSlot(index);
                return true;  // consume long press
            });
        }

        Button createCombo = findViewById(R.id.btnCreateCombo);
        if (createCombo != null) {
            createCombo.setOnClickListener(v -> showComboEditorForNextSlot());
        }
    }

    private void onComboSlotClicked(int index) {
        String[] keys = comboKeys[index];
        if (keys == null) {
            logToStatus("No combo set for this slot");
            return;
        }
        if (hidService == null || !hidService.isConnected()) {
            logToStatus("Not connected to device");
            return;
        }
        hidService.sendKeyPress(keys);   // multi-key version in your service
        logToStatus("Sent combo: " + String.join(" + ", keys));
    }

    private int findNextEmptyComboIndex() {
        for (int i = 0; i < MAX_COMBOS; i++) {
            if (comboKeys[i] == null) return i;
        }
        return -1;
    }

    private void showComboEditorForNextSlot() {
        int index = findNextEmptyComboIndex();
        if (index == -1) {
            logToStatus("All combo slots are full");
            return;
        }
        showComboEditorForIndex(index);
    }

    private void showComboEditorForIndex(int index) {
        android.app.AlertDialog.Builder builder =
                new android.app.AlertDialog.Builder(this);

        View dialogView = getLayoutInflater().inflate(
                R.layout.dialog_combo_editor, null);
        builder.setView(dialogView);

        CheckBox ctrlCheck = dialogView.findViewById(R.id.ctrlCheckbox);
        CheckBox shiftCheck = dialogView.findViewById(R.id.shiftCheckbox);
        CheckBox altCheck = dialogView.findViewById(R.id.altCheckbox);
        CheckBox guiCheck = dialogView.findViewById(R.id.guiCheckbox);
        android.widget.Spinner keySpinner =
                dialogView.findViewById(R.id.comboKeySpinner);
        Button saveButton = dialogView.findViewById(R.id.addComboButton);

        // Fill spinner with ALL_MAIN_KEYS
        android.widget.ArrayAdapter<String> adapter =
                new android.widget.ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        ALL_MAIN_KEYS
                );
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        keySpinner.setAdapter(adapter);

        android.app.AlertDialog dialog = builder.create();

        saveButton.setOnClickListener(v -> {
            java.util.ArrayList<String> parts = new java.util.ArrayList<>();

            if (ctrlCheck.isChecked()) parts.add("Ctrl");
            if (shiftCheck.isChecked()) parts.add("Shift");
            if (altCheck.isChecked()) parts.add("Alt");
            if (guiCheck.isChecked()) parts.add("GUI");

            String mainKey = (String) keySpinner.getSelectedItem();
            if (mainKey == null || mainKey.isEmpty()) {
                logToStatus("Pick a main key");
                return;
            }
            parts.add(mainKey);

            String[] combo = parts.toArray(new String[0]);
            comboKeys[index] = combo;
            saveCombos();

            Button slot = comboSlots[index];
            if (slot != null) {
                slot.setText(String.join("+", combo));
                slot.setVisibility(View.VISIBLE);
            }

            logToStatus("Combo " + (index + 1) + " set to: " +
                    String.join(" + ", combo));
            dialog.dismiss();
        });

        dialog.show();
    }

    private void saveCombos() {
        android.content.SharedPreferences prefs =
                getSharedPreferences("combos", MODE_PRIVATE);
        android.content.SharedPreferences.Editor e = prefs.edit();
        for (int i = 0; i < MAX_COMBOS; i++) {
            String key = "slot_" + i;
            if (comboKeys[i] == null) {
                e.remove(key);
            } else {
                // Join labels with a separator that never appears in labels
                e.putString(key, String.join(";", comboKeys[i]));
            }
        }
        e.apply();
    }

    private void loadCombos() {
        android.content.SharedPreferences prefs =
                getSharedPreferences("combos", MODE_PRIVATE);

        for (int i = 0; i < MAX_COMBOS; i++) {
            String stored = prefs.getString("slot_" + i, null);
            if (stored == null || stored.isEmpty()) {
                comboKeys[i] = null;
                if (comboSlots[i] != null) {
                    comboSlots[i].setText("");
                    comboSlots[i].setVisibility(View.GONE);
                }
                continue;
            }

            String[] parts = stored.split(";");
            comboKeys[i] = parts;

            Button slot = comboSlots[i];
            if (slot != null) {
                slot.setText(String.join("+", parts));
                slot.setVisibility(View.VISIBLE);
            }
        }
    }

    private void clearComboSlot(int index) {
        comboKeys[index] = null;
        Button slot = comboSlots[index];
        if (slot != null) {
            slot.setText("");
            slot.setVisibility(View.GONE);
        }
        logToStatus("Cleared combo slot " + (index + 1));
        saveCombos();   // <- ensure this is here
    }

    // ---------------- Service binding + lifecycle ----------------
    private void logToStatus(String message) {
        keyboardStatus.setText(message);
    }

    private void refreshShiftSensitiveLabels() {
        for (java.util.Map.Entry<Button, String> entry : baseLabels.entrySet()) {
            Button b = entry.getKey();
            String base = entry.getValue();
            String display = isShiftLatched ? shiftedCharFor(base) : base;
            b.setText(display);
        }
    }

    private String shiftedCharFor(String base) {
        switch (base) {
            case "1": return "!";
            case "2": return "@";
            case "3": return "#";
            case "4": return "$";
            case "5": return "%";
            case "6": return "^";
            case "7": return "&";
            case "8": return "*";
            case "9": return "(";
            case "0": return ")";
            case "-": return "_";
            case "=": return "+";
            case "[": return "{";
            case "]": return "}";
            case "\\": return "|";
            case ";": return ":";
            case "'": return "\"";
            case ",": return "<";
            case ".": return ">";
            case "/": return "?";
            default:
                // For letters: show uppercase when Shift is on
                if (base.length() == 1 && Character.isLetter(base.charAt(0))) {
                    return base.toUpperCase();
                }
                return base;
        }
    }

    private void updateShiftButtonsUi() {
        int color = isShiftLatched ? 0xFF008080 : 0xFF404040;
        Button left  = findViewById(R.id.keyShiftLeft);
        Button right = findViewById(R.id.keyShiftRight);
        if (left != null)  left.setBackgroundColor(color);
        if (right != null) right.setBackgroundColor(color);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BluetoothHIDService.LocalBinder binder =
                    (BluetoothHIDService.LocalBinder) service;
            hidService = binder.getService();
            isBound = true;
            logToStatus("HID Service ready");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            hidService = null;
            logToStatus("HID Service disconnected");
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    @Override
    public void onBackPressed() {
        logToStatus("Back pressed");
        super.onBackPressed();
    }
}
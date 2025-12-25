package com.example.hidcontroller;

import java.util.HashMap;
import java.util.Map;

public class HIDKeyCode {
    private static final Map<String, Integer> keyCodeMap = new HashMap<>();
    public static final byte MODIFIER_LEFT_CTRL   = 0x01;
    public static final byte MODIFIER_LEFT_SHIFT  = 0x02;
    public static final byte MODIFIER_LEFT_ALT    = 0x04;
    public static final byte MODIFIER_LEFT_GUI    = 0x08;


    static {
        // Letters A-Z (0x04-0x1D)
        keyCodeMap.put("A", 0x04); keyCodeMap.put("B", 0x05); keyCodeMap.put("C", 0x06);
        keyCodeMap.put("D", 0x07); keyCodeMap.put("E", 0x08); keyCodeMap.put("F", 0x09);
        keyCodeMap.put("G", 0x0A); keyCodeMap.put("H", 0x0B); keyCodeMap.put("I", 0x0C);
        keyCodeMap.put("J", 0x0D); keyCodeMap.put("K", 0x0E); keyCodeMap.put("L", 0x0F);
        keyCodeMap.put("M", 0x10); keyCodeMap.put("N", 0x11); keyCodeMap.put("O", 0x12);
        keyCodeMap.put("P", 0x13); keyCodeMap.put("Q", 0x14); keyCodeMap.put("R", 0x15);
        keyCodeMap.put("S", 0x16); keyCodeMap.put("T", 0x17); keyCodeMap.put("U", 0x18);
        keyCodeMap.put("V", 0x19); keyCodeMap.put("W", 0x1A); keyCodeMap.put("X", 0x1B);
        keyCodeMap.put("Y", 0x1C); keyCodeMap.put("Z", 0x1D);

        // Numbers 0-9 (0x1E-0x27)
        for (int i = 0; i <= 9; i++) {
            keyCodeMap.put(String.valueOf(i), 0x1E + i);
        }

        // Top row symbols + special keys
        keyCodeMap.put("Enter", 0x28); keyCodeMap.put("Esc", 0x29); keyCodeMap.put("Backspace", 0x2A);
        keyCodeMap.put("Tab", 0x2B); keyCodeMap.put("Space", 0x2C); keyCodeMap.put("-", 0x2D);
        keyCodeMap.put("=", 0x2E); keyCodeMap.put("[", 0x2F); keyCodeMap.put("]", 0x30);
        keyCodeMap.put("\\", 0x31); keyCodeMap.put("#", 0x32); keyCodeMap.put(";", 0x33);
        keyCodeMap.put("'", 0x34); keyCodeMap.put("`", 0x35); keyCodeMap.put(",", 0x36);
        keyCodeMap.put(".", 0x37); keyCodeMap.put("/", 0x38);

        // Function keys F1-F12 (0x3A-0x45)
        for (int i = 1; i <= 12; i++) {
            keyCodeMap.put("F" + i, 0x3A + (i - 1));
        }

        // Modifiers (special handling - stored separately)
        keyCodeMap.put("Ctrl", 0xE0); keyCodeMap.put("Shift", 0xE1);
        keyCodeMap.put("Alt", 0xE2); keyCodeMap.put("GUI", 0xE3);
    }


    public static int getHIDCode(String keyLabel) {
        return keyCodeMap.getOrDefault(keyLabel, 0x00);
    }

    public static boolean isValidKey(String keyLabel) {
        return keyCodeMap.containsKey(keyLabel) && keyCodeMap.get(keyLabel) != 0x00;
    }
}
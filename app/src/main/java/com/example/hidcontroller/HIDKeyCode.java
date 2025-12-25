package com.example.hidcontroller;

import java.util.HashMap;
import java.util.Map;

public class HIDKeyCode {
    private static final Map<String, Integer> keyCodeMap = new HashMap<>();

    static {
        // Letters (A-Z) - codes 0x04 to 0x1D
        keyCodeMap.put("A", 0x04);
        keyCodeMap.put("B", 0x05);
        keyCodeMap.put("C", 0x06);
        keyCodeMap.put("D", 0x07);
        keyCodeMap.put("E", 0x08);
        keyCodeMap.put("F", 0x09);
        keyCodeMap.put("G", 0x0A);
        keyCodeMap.put("H", 0x0B);
        keyCodeMap.put("I", 0x0C);
        keyCodeMap.put("J", 0x0D);
        keyCodeMap.put("K", 0x0E);
        keyCodeMap.put("L", 0x0F);
        keyCodeMap.put("M", 0x10);
        keyCodeMap.put("N", 0x11);
        keyCodeMap.put("O", 0x12);
        keyCodeMap.put("P", 0x13);
        keyCodeMap.put("Q", 0x14);
        keyCodeMap.put("R", 0x15);
        keyCodeMap.put("S", 0x16);
        keyCodeMap.put("T", 0x17);
        keyCodeMap.put("U", 0x18);
        keyCodeMap.put("V", 0x19);
        keyCodeMap.put("W", 0x1A);
        keyCodeMap.put("X", 0x1B);
        keyCodeMap.put("Y", 0x1C);
        keyCodeMap.put("Z", 0x1D);

        // Numbers (1-9, 0)
        keyCodeMap.put("1", 0x1E);
        keyCodeMap.put("2", 0x1F);
        keyCodeMap.put("3", 0x20);
        keyCodeMap.put("4", 0x21);
        keyCodeMap.put("5", 0x22);
        keyCodeMap.put("6", 0x23);
        keyCodeMap.put("7", 0x24);
        keyCodeMap.put("8", 0x25);
        keyCodeMap.put("9", 0x26);
        keyCodeMap.put("0", 0x27);

        // Special characters
        keyCodeMap.put("Enter", 0x28);
        keyCodeMap.put("Back", 0x2A);
        keyCodeMap.put("Tab", 0x2B);
        keyCodeMap.put("Space", 0x2C);
        keyCodeMap.put("-", 0x2D);
        keyCodeMap.put("=", 0x2E);
        keyCodeMap.put("[", 0x2F);
        keyCodeMap.put("]", 0x30);
        keyCodeMap.put("\\", 0x31);
        keyCodeMap.put(";", 0x33);
        keyCodeMap.put("'", 0x34);
        keyCodeMap.put("`", 0x35);
        keyCodeMap.put(",", 0x36);
        keyCodeMap.put(".", 0x37);
        keyCodeMap.put("/", 0x38);
    }

    public static int getHIDCode(String keyLabel) {
        return keyCodeMap.getOrDefault(keyLabel, 0x00);
    }

    public static boolean isValidKey(String keyLabel) {
        return keyCodeMap.containsKey(keyLabel) && keyCodeMap.get(keyLabel) != 0x00;
    }
}
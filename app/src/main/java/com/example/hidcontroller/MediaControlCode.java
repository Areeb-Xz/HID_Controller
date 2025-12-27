package com.example.hidcontroller;

import java.util.HashMap;
import java.util.Map;

public class MediaControlCode {

    private static final Map<String, Integer> mediaCodeMap = new HashMap<>();

    static {
        // HID Consumer Control codes (Usage IDs)
        mediaCodeMap.put("Mute", 0x00E2);
        mediaCodeMap.put("VolumeUp", 0x00E9);
        mediaCodeMap.put("VolumeDown", 0x00EA);

        mediaCodeMap.put("Play", 0x00CD);
        mediaCodeMap.put("Pause", 0x00CF);
        mediaCodeMap.put("PlayPause", 0x00CD);

        mediaCodeMap.put("Stop", 0x00B7);
        mediaCodeMap.put("Next", 0x00B5);
        mediaCodeMap.put("NextTrack", 0x00B5);
        mediaCodeMap.put("Previous", 0x00B6);
        mediaCodeMap.put("PrevTrack", 0x00B6);

        mediaCodeMap.put("FastForward", 0x00B3);
        mediaCodeMap.put("Rewind", 0x00B4);

        mediaCodeMap.put("Eject", 0x00B8);

        // Browser controls
        mediaCodeMap.put("Home", 0x0223);
        mediaCodeMap.put("Back", 0x0224);
        mediaCodeMap.put("Forward", 0x0225);
        mediaCodeMap.put("Refresh", 0x0227);

        // Application launcher
        mediaCodeMap.put("Calculator", 0x0192);
        mediaCodeMap.put("Email", 0x018A);
        mediaCodeMap.put("FileManager", 0x0194);

        // System power
        mediaCodeMap.put("PowerOff", 0x0030);
        mediaCodeMap.put("Sleep", 0x0032);
        mediaCodeMap.put("Wake", 0x0083);
    }

    public static int getConsumerCode(String controlName) {
        return mediaCodeMap.getOrDefault(controlName, 0x0000);
    }

    public static boolean isValidControl(String controlName) {
        return mediaCodeMap.containsKey(controlName) &&
                mediaCodeMap.get(controlName) != 0x0000;
    }

    public static byte[] codeToBytes(int code) {
        return new byte[]{
                (byte) (code & 0xFF),
                (byte) ((code >> 8) & 0xFF)
        };
    }

    public static int bytesToCode(byte[] bytes) {
        if (bytes == null || bytes.length < 2) {
            return 0x0000;
        }
        return ((bytes[1] & 0xFF) << 8) | (bytes[0] & 0xFF);
    }
}
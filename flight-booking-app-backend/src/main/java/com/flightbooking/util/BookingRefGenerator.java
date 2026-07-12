package com.flightbooking.util;

import java.security.SecureRandom;

public class BookingRefGenerator {
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no confusing chars (0,O,1,I)
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generate() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}

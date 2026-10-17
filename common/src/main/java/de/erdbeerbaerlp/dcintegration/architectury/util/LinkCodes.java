package de.erdbeerbaerlp.dcintegration.architectury.util;

import java.security.SecureRandom;

// 5-character link codes like K7Q2X. The core still keys pending links by int, so a code is just
// a number below 32^5 written in an alphabet without 0/O and 1/I. ~33.5 million codes, valid 10 minutes.
public final class LinkCodes {
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 5;
    public static final int SPACE = 32 * 32 * 32 * 32 * 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private LinkCodes() {}

    public static int next() {
        return RANDOM.nextInt(SPACE);
    }

    public static String encode(int number) {
        char[] out = new char[LENGTH];
        for (int i = LENGTH - 1; i >= 0; i--) {
            out[i] = ALPHABET.charAt(number % 32);
            number /= 32;
        }
        return new String(out);
    }

    // Throws NumberFormatException like Integer.parseInt, so the core's "not a valid code" reply still works
    public static int decode(String code) {
        String c = code.strip().toUpperCase();
        if (c.length() != LENGTH) throw new NumberFormatException(code);
        int number = 0;
        for (char ch : c.toCharArray()) {
            int digit = ALPHABET.indexOf(ch);
            if (digit < 0) throw new NumberFormatException(code);
            number = number * 32 + digit;
        }
        return number;
    }
}

package cz.vity.freerapid.plugins.services.keycaptcha;

import org.apache.commons.codec.binary.Hex;

import java.util.Random;

/**
 * @author ntoskrnl
 */
final class KeyCaptchaCrypt {

    public static String decrypt(final String dataAndSalt, final String keyData) throws Exception {
        final byte[] data = Hex.decodeHex(dataAndSalt.substring(0, dataAndSalt.length() - 8).toCharArray());
        final long salt = Long.parseLong(dataAndSalt.substring(dataAndSalt.length() - 8), 16);
        final byte[] key = generateKey(keyData, salt, data.length);
        xor(data, key);
        return new String(data, "ASCII");
    }

    public static String encrypt(final String dataString, final String keyData) throws Exception {
        final byte[] data = dataString.getBytes("ASCII");
        final long salt = new Random().nextInt(100000000);
        final byte[] key = generateKey(keyData, salt, data.length);
        xor(data, key);
        return Hex.encodeHexString(data) + String.format("%08x", salt);
    }

    private static void xor(final byte[] data, final byte[] key) {
        assert data.length == key.length;
        for (int i = 0; i < data.length; i++) {
            data[i] ^= key[i];
        }
    }

    private static byte[] generateKey(final String keyData, final long salt, final int dataLength) {
        final StringBuilder sb = new StringBuilder();
        for (final char c : keyData.toCharArray()) {
            sb.append((int) c);
        }
        final int multiplier = getMultiplier(sb);
        final int addend = (int) Math.round(keyData.length() / 3d);
        final int maxValue = Integer.MAX_VALUE;
        long value = getInitialValue(sb.append(salt).toString());
        final byte[] key = new byte[dataLength];
        for (int i = 0; i < key.length; i++) {
            value = ((multiplier * value) + addend) % maxValue;
            key[i] = (byte) Math.floor(((double) value / (double) maxValue) * 255d);
        }
        return key;
    }

    private static int getMultiplier(final CharSequence s) {
        final int pos = (int) Math.floor(s.length() / 5d);
        return Integer.parseInt("" + s.charAt(pos) + s.charAt(pos * 2) + s.charAt(pos * 3) + s.charAt(pos * 4) + s.charAt(pos * 5 - 1));
    }

    private static long getInitialValue(String s) {
        while (s.length() > 9) {
            final int i = Integer.parseInt(s.substring(0, 9)) + Integer.parseInt(s.substring(9, Math.min(s.length(), 14)));
            s = String.valueOf(i) + s.substring(Math.min(s.length(), 14), s.length());
        }
        return Long.parseLong(s);
    }

    private KeyCaptchaCrypt() {
    }

}

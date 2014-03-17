package cz.vity.freerapid.plugins.services.cryptography;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author ntoskrnl
 */
public class TestCrypto {

    private static final String KEY = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
    private static final String IV = "IVtestASDASDASDA";
    private static final String INPUT = "7CA3AC347D6F4C0E88207660241DA945";

    public static void main(String[] args) throws Exception {
        testJava();
        testBouncyCastle();
        testFrdCryptoAPI();
    }

    private static void testJava() throws Exception {
        // Requires the unlimited strength cryptography policy files to function
        final byte[] key = decodeHex(KEY);
        final byte[] iv = IV.getBytes("UTF-8");
        final byte[] input = decodeHex(INPUT);
        final SecretKeySpec spec = new SecretKeySpec(key, "AES");
        final IvParameterSpec ivSpec = new IvParameterSpec(iv);
        final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, spec, ivSpec);
        final byte[] decrypted = cipher.doFinal(input);
        System.out.println("Java:           " + new String(decrypted, "UTF-8"));
    }

    private static void testBouncyCastle() throws Exception {
        final byte[] key = decodeHex(KEY);
        final byte[] iv = IV.getBytes("UTF-8");
        final byte[] input = decodeHex(INPUT);
        final BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()));
        cipher.init(false, new ParametersWithIV(new KeyParameter(key), iv));
        final byte[] decrypted = new byte[cipher.getOutputSize(input.length)];
        int i = cipher.processBytes(input, 0, input.length, decrypted, 0);
        cipher.doFinal(decrypted, i);
        System.out.println("Bouncy Castle:  " + new String(decrypted, "UTF-8"));
    }

    private static void testFrdCryptoAPI() throws Exception {
        final String s = new CryptographySupport().setEngine(Engine.AES).setMode(Mode.CBC).setPadding(Padding.PKCS7).setKey(KEY).setIV(IV).decrypt(INPUT);
        System.out.println("FRD Crypto API: " + s);
    }

    private static byte[] decodeHex(final String value) {
        try {
            return Hex.decodeHex(value.toCharArray());
        } catch (DecoderException e) {
            throw new IllegalArgumentException(e);
        }
    }

}

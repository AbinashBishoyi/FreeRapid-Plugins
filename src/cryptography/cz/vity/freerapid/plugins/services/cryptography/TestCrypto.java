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

    private static final byte[] KEY = decodeHex("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
    private static final byte[] IV = "IVtestASDASDASDA".getBytes();
    private static final byte[] INPUT = decodeHex("7CA3AC347D6F4C0E88207660241DA945");

    public static void main(String[] args) throws Exception {
        testJava();
        testBouncyCastle();
    }

    private static void testJava() throws Exception {
        final SecretKeySpec spec = new SecretKeySpec(KEY, "AES");
        final IvParameterSpec ivSpec = new IvParameterSpec(IV);
        final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, spec, ivSpec);
        final byte[] decrypted = cipher.doFinal(INPUT);
        System.out.println("Java:          " + new String(decrypted, "UTF-8"));
    }

    private static void testBouncyCastle() throws Exception {
        BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()));
        cipher.init(false, new ParametersWithIV(new KeyParameter(KEY), IV));
        final byte[] decrypted = new byte[cipher.getOutputSize(INPUT.length)];
        int i = cipher.processBytes(INPUT, 0, INPUT.length, decrypted, 0);
        cipher.doFinal(decrypted, i);
        System.out.println("Bouncy Castle: " + new String(decrypted, "UTF-8"));
    }


    public static byte[] decodeHex(final String value) {
        try {
            return Hex.decodeHex(value.toCharArray());
        } catch (DecoderException e) {
            return new byte[0];
        }
    }

}

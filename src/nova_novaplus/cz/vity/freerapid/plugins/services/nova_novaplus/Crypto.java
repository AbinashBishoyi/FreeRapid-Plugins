package cz.vity.freerapid.plugins.services.nova_novaplus;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

/**
 * @author tong2shot
 */
class Crypto {
    private final static String PASSWORD = "EaDUutg4ppGYXwNMFdRJsadenFSnI6gJ";
    
    private SecretKey generateKey(byte[] pwBytes) throws Exception {
        byte[] _pwBytes = Arrays.copyOf(pwBytes, 16);
        SecretKey secretKey = new SecretKeySpec(_pwBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return new SecretKeySpec(cipher.doFinal(_pwBytes), "AES");
    }

    public String decrypt(String base64CipherText) throws Exception {
        byte[] cipherTextBytes = Base64.decodeBase64(base64CipherText);
        byte[] nonceBytes = Arrays.copyOf(Arrays.copyOf(cipherTextBytes, 8), 16);
        IvParameterSpec nonce = new IvParameterSpec(nonceBytes);
        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, generateKey(PASSWORD.getBytes()), nonce);
        byte[] decrypted = cipher.doFinal(cipherTextBytes, 8, cipherTextBytes.length - 8);
        return new String(decrypted);
    }
}

package cz.vity.freerapid.plugins.services.onetwothreevideo;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.Random;

/**
 * @author tong2shot
 */
public class Crypto {
    private final static String CRYPTO_MAGIC = "aIjcE4wFrUuQfCTc6CyLQb2gokmxN24JMMPS44Vdn7Q=";
    private final static String PK_GEN_MAGIC = "u@SP8VADiIZB5PaRmYg1TSddsO$LC%yd5G%zjsnumxSOR2*02L";

    private final String movieId;
    private final String salt;
    private final String publicKey;

    public Crypto(String movieId) {
        this.movieId = movieId;
        this.salt = generateSalt();
        this.publicKey = generatePublicKey(this.movieId, this.salt);
    }

    public Crypto(String movieId, String publicKey, String salt) {
        this.movieId = movieId;
        this.salt = salt;
        this.publicKey = publicKey;
    }

    private String generateSalt() {
        final int length = 16;
        byte[] bytes = new byte[length];
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) (random.nextInt(254) + 1);
        }
        return Base64.encodeBase64String(bytes);
    }

    private String generatePublicKey(String movieId, String salt) {
        String data = movieId + PK_GEN_MAGIC + salt;
        return DigestUtils.md5Hex(data);
    }

    private byte[] process(byte[] data, String key) {
        final int length = data.length;
        byte[] ret = new byte[length];
        int i = 0, j = 0;
        while (i < length) {
            ret[i] = (byte) (data[i] ^ ((byte) key.charAt(j)));
            j++;
            if (j >= key.length()) {
                j = 0;
            }
            i++;
        }
        return ret;
    }

    private String encrypt(String data, String key) {
        return Base64.encodeBase64String(process(data.getBytes(), key));
    }

    private String encrypt(String data, String key, String salt) {
        return Base64.encodeBase64String((Base64.encodeBase64String(process(data.getBytes(), DigestUtils.md5Hex(salt + key))) + salt).getBytes());
    }

    private String decrypt(String data, String key, boolean salted) {
        if (salted) {
            final int saltLength = 24;
            String decoded = new String(Base64.decodeBase64(data));
            data = decoded.substring(0, decoded.length() - saltLength);
            String salt = decoded.substring(decoded.length() - saltLength);
            return new String(process(Base64.decodeBase64(data), DigestUtils.md5Hex(salt + key)));
        } else {
            return new String(process(Base64.decodeBase64(data), key));
        }
    }

    public String encryptWithKey(String data) {
        return encryptWithKey(data, publicKey);
    }

    public String encryptWithKey(String data, String publicKey) {
        String encryptionKey = publicKey + CRYPTO_MAGIC;
        return encrypt(data, encryptionKey, salt);
    }

    public String decryptWithKey(String data) {
        return decryptWithKey(data, publicKey);
    }

    public String decryptWithKey(String data, String publicKey) {
        String decryptionKey = publicKey + CRYPTO_MAGIC;
        return decrypt(data, decryptionKey, true);
    }

    public String getMovieId() {
        return movieId;
    }

    public String getSalt() {
        return salt;
    }

    public String getPublicKey() {
        return publicKey;
    }

}

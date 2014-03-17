package cz.vity.freerapid.plugins.services.cryptography;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.engines.*;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.modes.CFBBlockCipher;
import org.bouncycastle.crypto.modes.OFBBlockCipher;
import org.bouncycastle.crypto.modes.SICBlockCipher;
import org.bouncycastle.crypto.paddings.*;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * Helper class for cryptography. Allows unlimited strength of keys.
 * Instead of using the Bouncy Castle classes directly, this should be used.
 * <br />
 * Instances of this class can be reused.
 * <br />
 * Method chaining is possible, as the setters return this instance.
 * <br />
 * Only block ciphers are supported at the moment.
 *
 * @author ntoskrnl
 */
public class CryptographySupport {
    private Engine engine;
    private Mode mode;
    private Padding padding;
    private byte[] key;
    private byte[] iv;

    /**
     * Creates a new instance.
     */
    public CryptographySupport() {
    }

    /**
     * Decrypts a string.
     *
     * @param input hex string to decrypt
     * @return decrypted string
     * @throws Exception if something goes wrong
     */
    public String decrypt(String input) throws Exception {
        return new String(decrypt(decodeHex(input)), "UTF-8");
    }

    /**
     * Decrypts an array of bytes.
     *
     * @param input bytes to decrypt
     * @return decrypted bytes
     * @throws Exception if something goes wrong
     */
    public byte[] decrypt(byte[] input) throws Exception {
        BufferedBlockCipher cipher = getCipher();
        cipher.init(false, getParameters());
        byte[] output = new byte[cipher.getOutputSize(input.length)];
        int i = cipher.processBytes(input, 0, input.length, output, 0);
        cipher.doFinal(output, i);
        return output;
    }

    /**
     * Encrypts a string.
     *
     * @param input string to encrypt
     * @return string of hex characters consisting of encrypted content
     * @throws Exception if something goes wrong
     */
    public String encrypt(String input) throws Exception {
        return encodeHex(encrypt(input.getBytes("UTF-8")));
    }

    /**
     * Encrypts an array of bytes.
     *
     * @param input bytes to encrypt
     * @return encrypted bytes
     * @throws Exception if something goes wrong
     */
    public byte[] encrypt(byte[] input) throws Exception {
        BufferedBlockCipher cipher = getCipher();
        cipher.init(true, getParameters());
        byte[] output = new byte[cipher.getOutputSize(input.length)];
        int i = cipher.processBytes(input, 0, input.length, output, 0);
        cipher.doFinal(output, i);
        return output;
    }

    public CryptographySupport setEngine(Engine engine) {
        this.engine = engine;
        return this;
    }

    public CryptographySupport setMode(Mode mode) {
        this.mode = mode;
        return this;
    }

    public CryptographySupport setPadding(Padding padding) {
        this.padding = padding;
        return this;
    }

    /**
     * @param key hex string
     * @return <code>this</code>
     */
    public CryptographySupport setKey(String key) {
        this.key = decodeHex(key);
        return this;
    }

    /**
     * @param iv string whose UTF bytes are the IV
     * @return <code>this</code>
     */
    public CryptographySupport setIV(String iv) {
        try {
            this.iv = iv.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            //should never happen
        }
        return this;
    }

    public CryptographySupport setKey(byte[] key) {
        this.key = Arrays.copyOf(key, key.length);
        return this;
    }

    public CryptographySupport setIV(byte[] iv) {
        this.iv = Arrays.copyOf(iv, iv.length);
        return this;
    }

    private BufferedBlockCipher getCipher() {
        BlockCipher cipher = getEngine();
        if (mode != null) {
            cipher = getMode(cipher);
        }
        if (padding != null) {
            return new PaddedBufferedBlockCipher(cipher, getPadding());
        } else {
            return new BufferedBlockCipher(cipher);
        }
    }

    private BlockCipher getEngine() {
        switch (engine) {
            case AES:
                return new AESLightEngine();
            case Blowfish:
                return new BlowfishEngine();
            case DES:
                return new DESEngine();
            case TripleDES:
                return new DESedeEngine();
            case Serpent:
                return new SerpentEngine();
            case Twofish:
                return new TwofishEngine();
            default:
                throw new IllegalStateException("Engine not set");
        }
    }

    private BlockCipher getMode(BlockCipher engine) {
        switch (mode) {
            case CBC:
                return new CBCBlockCipher(engine);
            case CFB:
                return new CFBBlockCipher(engine, 32);
            case OFB:
                return new OFBBlockCipher(engine, 32);
            case SIC:
                return new SICBlockCipher(engine);
            default:
                throw new IllegalStateException("Mode not set");
        }
    }

    private BlockCipherPadding getPadding() {
        switch (padding) {
            case ISO10126d2:
                return new ISO10126d2Padding();
            case ISO7816d4:
                return new ISO7816d4Padding();
            case PKCS7:
                return new PKCS7Padding();
            case TBC:
                return new TBCPadding();
            case X923:
                return new X923Padding();
            case ZeroByte:
                return new ZeroBytePadding();
            default:
                throw new IllegalStateException("Padding not set");
        }
    }

    private CipherParameters getParameters() {
        if (key == null) {
            throw new IllegalStateException("Key not set");
        }
        CipherParameters parameters = new KeyParameter(key);
        if (iv != null) {
            return new ParametersWithIV(parameters, iv);
        } else {
            return parameters;
        }
    }

    private static byte[] decodeHex(final String value) {
        try {
            return Hex.decodeHex(value.toCharArray());
        } catch (DecoderException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static String encodeHex(final byte[] value) {
        return new String(Hex.encodeHex(value));
    }

}

package cz.vity.freerapid.plugins.services.ulozto.captcha;

/**
 * @author tong2shot
 */
public class Fingerprint {
    private char character;
    private byte[] fingerprint;

    public Fingerprint(char character, byte[] fingerprint) {
        this.character = character;
        this.fingerprint = fingerprint;
    }

    public char getCharacter() {
        return character;
    }

    public byte[] getFingerprint() {
        return fingerprint;
    }
}

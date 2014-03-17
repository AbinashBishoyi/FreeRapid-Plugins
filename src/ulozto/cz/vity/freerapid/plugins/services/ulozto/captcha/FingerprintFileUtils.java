package cz.vity.freerapid.plugins.services.ulozto.captcha;

import com.musicg.wave.Wave;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * @author tong2shot
 */
public class FingerprintFileUtils {

    public static final String CAPTCHA_DIR = "/media/DATA/kerja/javaProj/FRD/frd/captcha/ulozto/2/";
    public static final String CAPTCHA_ALPHA_DIR = CAPTCHA_DIR + "alpha/";
    public static final String FINGERPRINT_OUT_FILE = CAPTCHA_DIR + "fingerprint.bin";

    private FingerprintFileUtils() {
    }

    public static void populateFingerprintList(String dirName, List<Fingerprint> fingerprintList) {
        for (int i = 0; i < 26; i++) {
            if ((i == 24) || (i == 22)) continue; //skip 'w' and 'y', couldn't find sample
            char character = (char) (i + 97);

            Wave wave = new Wave(dirName + Character.toString(character) + ".wav");
            byte[] fingerprintBytes = wave.getFingerprint();

            Fingerprint fingerprint = new Fingerprint(character, fingerprintBytes);
            fingerprintList.add(fingerprint);
        }
    }

    public static void save(String fname, List<Fingerprint> fingerprintList) throws PluginImplementationException {
        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(new FileOutputStream(fname));
            dos.writeInt(fingerprintList.size());
            for (Fingerprint fingerprint : fingerprintList) {
                dos.writeChar(fingerprint.getCharacter());
                int fingerprintLength = fingerprint.getFingerprint().length;
                dos.writeInt(fingerprintLength);
                for (int i = 0; i < fingerprintLength; i++) {
                    dos.writeByte(fingerprint.getFingerprint()[i]);
                }

            }
        } catch (IOException ex) {
            throw new PluginImplementationException("Saving fingerprint failed");
        } finally {
            try {
                if (dos != null) {
                    dos.close();
                }
            } catch (IOException ex) {
                //
            }
        }
    }

    public static void load(String fname, List<Fingerprint> fingerprintList) throws PluginImplementationException {
        DataInputStream dis = null;
        try {
            dis = new DataInputStream(FingerprintFileUtils.class.getResourceAsStream(fname));
            //dis = new DataInputStream(new FileInputStream(fname));
            fingerprintList.clear();
            int numberOfChars = dis.readInt();
            for (int i = 0; i < numberOfChars; i++) {
                char character = dis.readChar();
                int fingerprintLength = dis.readInt();
                byte fingerprintBytes[] = new byte[fingerprintLength];
                for (int j = 0; j < fingerprintLength; j++) {
                    fingerprintBytes[j] = dis.readByte();
                }
                Fingerprint fingerprint = new Fingerprint(character, fingerprintBytes);
                fingerprintList.add(fingerprint);
            }
        } catch (IOException ex) {
            throw new PluginImplementationException("Loading fingerprint failed");
        } finally {
            try {
                if (dis != null) {
                    dis.close();
                }
            } catch (IOException e) {
                //
            }
        }
    }

}

package cz.vity.freerapid.plugins.services.ulozto.captcha;

import com.musicg.wave.Wave;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;

import java.io.*;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * @author tong2shot
 */
public class FingerprintFileUtils {

    //public static final String CAPTCHA_DIR = "/media/DATA/kerja/javaProj/FRD/frd/captcha/ulozto/3/";
    //public static final String CAPTCHA_ALPHA_DIR = CAPTCHA_DIR + "alpha/";
    //public static final String FINGERPRINT_OUT_FILE = CAPTCHA_DIR + "fingerprint.bin";

    private FingerprintFileUtils() {
    }

    public static void populateFingerprintList(String dirName, List<Fingerprint> fingerprintList) {
        File file;
        for (int i = 0; i < 26; i++) {
            //if ((i == 24) || (i == 22)) continue; //skip 'w' and 'y', couldn't find sample
            char character = (char) (i + 97);
            String fname = dirName + Character.toString(character) + ".wav";
            file = new File(fname);
            if (file.exists()) {
                Wave wave = new Wave(fname);
                byte[] fingerprintBytes = wave.getFingerprint();

                Fingerprint fingerprint = new Fingerprint(character, fingerprintBytes);
                fingerprintList.add(fingerprint);
            }
        }
    }

    public static void save(String fname, List<Fingerprint> fingerprintList) throws PluginImplementationException {
        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(new DeflaterOutputStream(new FileOutputStream(fname)));
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
            dis = new DataInputStream(new InflaterInputStream(FingerprintFileUtils.class.getResourceAsStream(fname)));
            //dis = new DataInputStream(new InflaterInputStream(new FileInputStream(fname)));
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

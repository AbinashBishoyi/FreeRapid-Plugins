package cz.vity.freerapid.plugins.services.ulozto.captcha;

import com.musicg.fingerprint.FingerprintSimilarity;
import com.musicg.fingerprint.FingerprintSimilarityComputer;
import com.musicg.wave.Wave;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Class for reading Uloz.to sound captcha
 *
 * @author JPEXS
 * @author tong2shot
 */

public class SoundReader {
    private final static Logger logger = Logger.getLogger(SoundReader.class.getName());
    public static final int CAPTCHA_LENGTH = 4;

    private static final List<Fingerprint> fingerprintList = new LinkedList<Fingerprint>();

    static {
        try {
            FingerprintFileUtils.load("/resources/fingerprint.bin", fingerprintList);
            //FingerprintFileUtils.load(FingerprintFileUtils.FINGERPRINT_OUT_FILE, fingerprintList);
        } catch (PluginImplementationException e) {
            //
        }
    }

    public String parse(InputStream isCaptcha) {
        Wave waveCaptcha = new Wave(isCaptcha);
        byte[] captchaFingerprint = waveCaptcha.getFingerprint();

        List<SoundPattern> soundPatternList = new LinkedList<SoundPattern>();
        FingerprintSimilarityComputer fpsc;
        FingerprintSimilarity fps;
        for (Fingerprint fingerprint : fingerprintList) {
            fpsc = new FingerprintSimilarityComputer(captchaFingerprint, fingerprint.getFingerprint());
            fps = fpsc.getFingerprintsSimilarity();

            char character = fingerprint.getCharacter();
            float similarity = fps.getSimilarity();
            float score = fps.getScore();
            float timePosition = fps.getsetMostSimilarTimePosition();

            SoundPattern soundPattern = new SoundPattern();
            soundPattern.character = character;
            soundPattern.fingerprintSimilarity = similarity;
            soundPattern.score = score;
            soundPattern.timePosition = timePosition;
            soundPatternList.add(soundPattern);

            logger.info(String.format("%c: sim=%.2f sc=%.2f time=%.2f", character, similarity, score, timePosition));
        }

        Collections.sort(soundPatternList);
        Collections.reverse(soundPatternList);
        List<CharPos> charPosList = new LinkedList<CharPos>();
        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            SoundPattern fp = soundPatternList.get(i);
            charPosList.add(new CharPos(fp.character, fp.timePosition));
        }

        Collections.sort(charPosList);
        StringBuilder sb = new StringBuilder(CAPTCHA_LENGTH);
        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            sb.append(charPosList.get(i).character);
        }

        return sb.toString();
    }

    private static class SoundPattern implements Comparable<SoundPattern> {
        private char character;
        private byte[] fingerprint;
        private float fingerprintSimilarity;
        private float score;
        private float timePosition;

        @Override
        public int compareTo(SoundPattern that) {
            return Float.compare(this.score, that.score);
        }
    }

    private static class CharPos implements Comparable<CharPos> {
        private char character;
        private float timePosition;

        private CharPos(char character, float timePosition) {
            this.character = character;
            this.timePosition = timePosition;
        }

        @Override
        public int compareTo(CharPos that) {
            return Float.compare(this.timePosition, that.timePosition);
        }
    }
}
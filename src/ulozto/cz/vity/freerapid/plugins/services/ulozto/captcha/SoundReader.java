package cz.vity.freerapid.plugins.services.ulozto.captcha;

import com.musicg.fingerprint.FingerprintSimilarityMultiplePosition;
import com.musicg.fingerprint.FingerprintSimilarityMultiplePositionComputer;
import com.musicg.wave.Wave;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;

import java.io.InputStream;
import java.util.*;
import java.util.logging.Logger;

/**
 * Class for reading Uloz.to sound captcha
 *
 * @author JPEXS
 * @author tong2shot
 */

public class SoundReader {
    private final static Logger logger = Logger.getLogger(SoundReader.class.getName());
    private final static float MIN_TIME_DIFF = 0.2f;
    private final static int ADDITIONAL_STORAGE_FACTOR = 2;
    private final static int CAPTCHA_LENGTH = 4;
    private final static List<Fingerprint> fingerprintList = new LinkedList<Fingerprint>();

    static {
        try {
            FingerprintFileUtils.load("/resources/fingerprint.bin", fingerprintList);
            //FingerprintFileUtils.load(FingerprintFileUtils.FINGERPRINT_OUT_FILE, fingerprintList);
        } catch (PluginImplementationException e) {
            //
        }
    }

    public String parse(InputStream isCaptcha) throws PluginImplementationException {
        if (fingerprintList.isEmpty()) {
            throw new PluginImplementationException("Fingerprint database is empty");
        }

        Wave waveCaptcha = new Wave(isCaptcha);
        byte[] captchaFingerprint = waveCaptcha.getFingerprint();

        List<SoundPattern> soundPatternList = new ArrayList<SoundPattern>();
        FingerprintSimilarityMultiplePositionComputer fpsc;
        FingerprintSimilarityMultiplePosition fps;
        for (Fingerprint fingerprint : fingerprintList) {
            fpsc = new FingerprintSimilarityMultiplePositionComputer(captchaFingerprint, fingerprint.getFingerprint());
            fps = fpsc.getFingerprintsSimilarity(CAPTCHA_LENGTH);

            char character = fingerprint.getCharacter();
            //float[] similarities = fps.getSimilarities();
            float[] scores = fps.getScores();
            float[] timePositions = fps.getsetMostSimilarTimePositions();


            for (int i = 0; i < CAPTCHA_LENGTH; i++) {
                SoundPattern sp = new SoundPattern(character, scores[i], timePositions[i]);
                if (!soundPatternList.contains(sp)) { //not too close
                    soundPatternList.add(sp);
                    //logger.info(String.format("%c: sim=%.2f sc=%.2f time=%f", character, similarities[i], scores[i], timePositions[i]));
                } else { //too close
                    int spInListIdx = soundPatternList.indexOf(sp); //get index of the similar sound pattern, similar->look at equality
                    SoundPattern spInList = soundPatternList.get(spInListIdx);
                    if (sp.score > spInList.score) {
                        soundPatternList.set(spInListIdx, sp); //replace sound pattern with the better one
                    }
                }
            }
        }

        //sorted by score, descending
        CharScoreComparator csc = new CharScoreComparator();
        Collections.sort(soundPatternList, csc);
        Collections.reverse(soundPatternList);

        //sorted by pos, ascending, limit=CAPTCHA_LENGTH*ADDITIONAL_STORAGE_FACTOR
        List<SoundPattern> charPosList = new ArrayList<SoundPattern>(soundPatternList.subList(0, CAPTCHA_LENGTH * ADDITIONAL_STORAGE_FACTOR));
        CharPosComparator cpp = new CharPosComparator();
        Collections.sort(charPosList, cpp);

        //remove sound pattern that is too close each other
        for (int i = 0; i < charPosList.size(); ) {
            if (i > 0) {
                SoundPattern cp = charPosList.get(i);
                SoundPattern cpPrev = charPosList.get(i - 1);
                float timeDiff = (cp.timePosition - cpPrev.timePosition);
                if (timeDiff < MIN_TIME_DIFF) { //too close
                    if (cp.score > cpPrev.score) { //score as removal criteria
                        charPosList.remove(i - 1);
                    } else {
                        charPosList.remove(i);
                    }
                } else {
                    i++;
                }
            } else {
                i++;
            }
        }

        //sorted by score, descending
        soundPatternList = new ArrayList<SoundPattern>(charPosList);
        Collections.sort(soundPatternList, csc);
        Collections.reverse(soundPatternList);

        //sorted by pos, ascending, limit=CAPTCHA_LENGTH
        try {
            charPosList = new ArrayList<SoundPattern>(soundPatternList.subList(0, CAPTCHA_LENGTH));
        } catch (Exception e) {
            throw new PluginImplementationException("Sound patterns size less than captcha length");
        }
        Collections.sort(charPosList, cpp);

        StringBuilder sb = new StringBuilder(CAPTCHA_LENGTH);
        float minTimeDiff = Float.MAX_VALUE;
        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            SoundPattern cp = charPosList.get(i);
            if (i > 0) {
                SoundPattern cpPrev = charPosList.get(i - 1);
                float timeDiff = (cp.timePosition - cpPrev.timePosition);
                if (timeDiff < minTimeDiff) {
                    minTimeDiff = timeDiff;
                }
            }
            sb.append(charPosList.get(i).character);
        }
        logger.info("Min time diff : " + minTimeDiff);

        return sb.toString();
    }

    private static class SoundPattern {
        private char character;
        private float score;
        private float timePosition;

        private SoundPattern(char character, float score, float timePosition) {
            this.character = character;
            this.score = score;
            this.timePosition = timePosition;
        }

        @Override
        public boolean equals(Object that) {
            return (that != null
                    && that instanceof SoundPattern
                    && ((SoundPattern) that).character == this.character
                    && (Math.abs(((SoundPattern) that).timePosition - this.timePosition) < MIN_TIME_DIFF));
        }
    }

    private static class CharScoreComparator implements Comparator<SoundPattern> {
        @Override
        public int compare(SoundPattern o1, SoundPattern o2) {
            return Float.compare(o1.score, o2.score);
        }
    }

    private static class CharPosComparator implements Comparator<SoundPattern> {
        @Override
        public int compare(SoundPattern o1, SoundPattern o2) {
            return Float.compare(o1.timePosition, o2.timePosition);
        }
    }
}
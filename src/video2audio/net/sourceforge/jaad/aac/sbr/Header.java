package net.sourceforge.jaad.aac.sbr;

import net.sourceforge.jaad.aac.AACException;
import net.sourceforge.jaad.aac.syntax.BitStream;

class Header {

    private boolean initialized;
    private int ampRes, startFreq, stopFreq, xOverBand;
    private int freqScale, alterScale, noiseBands; //extra header 1
    private int limiterBands, limiterGains, interpolFreq, smoothingMode; //extra header 2
    private boolean reset;

    Header() {
        reset = false;
        initialized = false;
    }

    void decode(BitStream in) throws AACException {
        initialized = true;

        int startFreqOld = startFreq;
        int stopFreqOld = stopFreq;
        int freqScaleOld = freqScale;
        int alterScaleOld = alterScale;
        int xOverBandOld = xOverBand;
        int noiseBandsOld = noiseBands;

        ampRes = in.readBit();
        startFreq = in.readBits(4);
        stopFreq = in.readBits(4);
        xOverBand = in.readBits(3);

        in.skipBits(2); //reserved

        boolean extraHeader1 = in.readBool();
        boolean extraHeader2 = in.readBool();

        if (extraHeader1) {
            freqScale = in.readBits(2);
            alterScale = in.readBit();
            noiseBands = in.readBits(2);
        } else {
            freqScale = 2;
            alterScale = 1;
            noiseBands = 2;
        }

        if (extraHeader2) {
            limiterBands = in.readBits(2);
            limiterGains = in.readBits(2);
            interpolFreq = in.readBit();
            smoothingMode = in.readBit();
        } else {
            limiterBands = 2;
            limiterGains = 2;
            interpolFreq = 1;
            smoothingMode = 1;
        }

        reset = (startFreqOld != startFreq) || (stopFreqOld != stopFreq)
                || (freqScaleOld != freqScale) || (alterScaleOld != alterScale)
                || (xOverBandOld != xOverBand) || (noiseBandsOld != noiseBands);
    }

    public boolean isInitialized() {
        return initialized;
    }

    int getAmpRes() {
        return ampRes;
    }

    void setAmpRes(int ampRes) {
        this.ampRes = ampRes;
    }

    int getStartFreq() {
        return startFreq;
    }

    int getStopFreq() {
        return stopFreq;
    }

    int getXOverBand() {
        return xOverBand;
    }

    int getFreqScale() {
        return freqScale;
    }

    int getAlterScale() {
        return alterScale;
    }

    int getNoiseBands() {
        return noiseBands;
    }

    int getLimiterBands() {
        return limiterBands;
    }

    int getLimiterGains() {
        return limiterGains;
    }

    int getInterpolFreq() {
        return interpolFreq;
    }

    int getSmoothingMode() {
        return smoothingMode;
    }

    boolean isReset() {
        return reset;
    }
}

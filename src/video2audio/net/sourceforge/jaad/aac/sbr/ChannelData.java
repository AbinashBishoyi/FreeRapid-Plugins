package net.sourceforge.jaad.aac.sbr;

import net.sourceforge.jaad.aac.AACException;
import net.sourceforge.jaad.aac.syntax.BitStream;

import java.util.Arrays;

class ChannelData implements Constants, HuffmanTables {

    private final int channel;
    private int ampRes;
    private int frameClass;
    private int numEnv, varBord0, varBord1, numRel0, numRel1, numNoise, pointer;
    private int numEnvPrev;
    private int[] freqRes, relBord0, relBord1;
    private int freqResPrev;
    private int[] dfEnv, dfNoise, invfMode;
    private int[] invfModePrev;
    private int[][] dataEnv, dataNoise;
    private boolean harmonicsPresent;
    private int[] addHarmonics;
    private int[] te, tq, tePrev;
    private int[][] envelopeScalefactors, noiseFloorData;
    private int[] envelopeScalefactorsPrev, noiseFloorDataPrev;
    private float[][] Eorig, Qorig;
    private float[] bwArray, bwArrayPrev;
    private int lTemp, La, LaPrev;
    private int[] sIndexMappedPrev;

    ChannelData(int channel) {
        this.channel = channel;

        freqResPrev = 0;
        envelopeScalefactorsPrev = new int[50]; //TODO: max. size
        noiseFloorDataPrev = new int[50]; //TODO: max. size
        tePrev = new int[1];
        numEnvPrev = 0;
        invfModePrev = new int[50]; //TODO: max. size
        bwArrayPrev = new float[50]; //TODO: max. size
        lTemp = 0;
        sIndexMappedPrev = new int[0];
        LaPrev = 0;
    }

    void decodeGrid(BitStream in, Header header, FrequencyTables tables) throws AACException {
        int len = tables.getFTableHigh()[tables.getNHigh()];
        if (sIndexMappedPrev.length < len) {
            int[] tmp = new int[len];
            System.arraycopy(sIndexMappedPrev, 0, tmp, 0, sIndexMappedPrev.length);
            Arrays.fill(tmp, sIndexMappedPrev.length, tmp.length - 1, 0);
            sIndexMappedPrev = tmp;
        } else if (sIndexMappedPrev.length > len) {
            //TODO: needed?
            int[] tmp = new int[len];
            System.arraycopy(sIndexMappedPrev, 0, tmp, 0, len);
            sIndexMappedPrev = tmp;
        }

        ampRes = header.getAmpRes();

        frameClass = in.readBits(2);
        int absBordLead, absBordTrail, nRelLead, nRelTrail;
        int[] relBordLead = null, relBordTrail = null;
        switch (frameClass) {
            case FIXFIX:
                numEnv = 1 << in.readBits(2);
                if (numEnv == 1) ampRes = 0;

                freqRes = new int[numEnv];
                freqRes[0] = in.readBit();
                for (int env = 1; env < numEnv; env++) {
                    freqRes[env] = freqRes[0];
                }

                absBordLead = 0;
                absBordTrail = TIME_SLOTS[0];
                nRelLead = numEnv - 1;
                relBordLead = new int[nRelLead];
                Arrays.fill(relBordLead, (int) Math.round((double) TIME_SLOTS[0] / (double) numEnv));
                nRelTrail = 0;

                La = -1;
                break;
            case FIXVAR:
                varBord1 = in.readBits(2);
                numRel1 = in.readBits(2);
                numEnv = numRel1 + 1;

                relBord1 = new int[numEnv - 1];
                for (int rel = 0; rel < numEnv - 1; rel++) {
                    relBord1[rel] = 2 * in.readBits(2) + 2;
                }

                pointer = in.readBits(CEIL_LOG2[numEnv]);

                freqRes = new int[numEnv];
                for (int env = 0; env < numEnv; env++) {
                    freqRes[numEnv - 1 - env] = in.readBit();
                }

                absBordLead = 0;
                absBordTrail = varBord1 + TIME_SLOTS[0];
                nRelLead = 0;
                nRelTrail = numRel1;
                relBordTrail = new int[nRelTrail];
                System.arraycopy(relBord1, 0, relBordTrail, 0, nRelTrail);

                La = pointer == 0 ? -1 : numEnv + 1 - pointer;
                break;
            case VARFIX:
                varBord0 = in.readBits(2);
                numRel0 = in.readBits(2);
                numEnv = numRel0 + 1;

                relBord0 = new int[numEnv - 1];
                for (int rel = 0; rel < numEnv - 1; rel++) {
                    relBord0[rel] = 2 * in.readBits(2) + 2;
                }

                pointer = in.readBits(CEIL_LOG2[numEnv]);

                freqRes = new int[numEnv];
                for (int env = 0; env < numEnv; env++) {
                    freqRes[env] = in.readBit();
                }

                absBordLead = varBord0;
                absBordTrail = TIME_SLOTS[0];
                nRelLead = numRel0;
                relBordLead = new int[nRelLead];
                System.arraycopy(relBord0, 0, relBordLead, 0, nRelLead);
                nRelTrail = 0;

                La = pointer > 1 ? pointer - 1 : -1;
                break;
            default: //VARVAR
                varBord0 = in.readBits(2);
                varBord1 = in.readBits(2);
                numRel0 = in.readBits(2);
                numRel1 = in.readBits(2);
                numEnv = numRel0 + numRel1 + 1;

                relBord0 = new int[numRel0];
                for (int rel = 0; rel < numRel0; rel++) {
                    relBord0[rel] = 2 * in.readBits(2) + 2;
                }
                relBord1 = new int[numRel1];
                for (int rel = 0; rel < numRel1; rel++) {
                    relBord1[rel] = 2 * in.readBits(2) + 2;
                }

                pointer = in.readBits(CEIL_LOG2[numEnv]);

                freqRes = new int[numEnv];
                for (int env = 0; env < numEnv; env++) {
                    freqRes[env] = in.readBit();
                }

                absBordLead = varBord0;
                absBordTrail = varBord1 + TIME_SLOTS[0];
                nRelLead = numRel0;
                relBordLead = new int[nRelLead];
                System.arraycopy(relBord0, 0, relBordLead, 0, nRelLead);
                nRelTrail = numRel1;
                relBordTrail = new int[nRelTrail];
                System.arraycopy(relBord1, 0, relBordTrail, 0, nRelTrail);

                La = pointer == 0 ? -1 : numEnv + 1 - pointer;
                break;
        }

        if (numEnv > 1) numNoise = 2;
        else numNoise = 1;

        te = new int[numEnv + 1];
        te[0] = 0;
        int sum;
        for (int l = 1; l < numEnv; l++) {
            sum = 0;
            if (l <= nRelLead) {
                sum = absBordLead;
                for (int i = 0; i < l; i++) {
                    sum += relBordLead[i];
                }
            } else {
                for (int i = 0; i < numEnv - l - 1; i++) {
                    sum += relBordTrail[i];
                }
                sum = absBordTrail - sum;
            }
            te[l] = sum;
        }
        te[numEnv] = absBordTrail;

        if (numEnv == 1) tq = new int[]{te[0], te[1]};
        else {
            int middleBorder;
            if (frameClass == FIXFIX) middleBorder = numEnv / 2;
            else if (frameClass == VARFIX) {
                if (pointer == 0) middleBorder = 1;
                else if (pointer == 1) middleBorder = numEnv - 1;
                else middleBorder = pointer - 1;
            } else {
                if (pointer <= 1) middleBorder = numEnv - 1;
                else middleBorder = numEnv + 1 - pointer;
            }
            tq = new int[]{te[0], te[middleBorder], te[numEnv]};
        }
    }

    void copyGrid(ChannelData cd) {
        ampRes = cd.ampRes;
        frameClass = cd.frameClass;
        numEnv = cd.numEnv;
        numNoise = cd.numNoise;
        pointer = cd.pointer;

        freqRes = new int[cd.freqRes.length];
        System.arraycopy(cd.freqRes, 0, freqRes, 0, cd.freqRes.length);
        te = new int[cd.te.length];
        System.arraycopy(cd.te, 0, te, 0, te.length);
        tq = new int[cd.tq.length];
        System.arraycopy(cd.tq, 0, tq, 0, tq.length);
    }

    void decodeDTDF(BitStream in) throws AACException {
        dfEnv = new int[numEnv];
        for (int env = 0; env < numEnv; env++) {
            dfEnv[env] = in.readBit();
        }

        dfNoise = new int[numNoise];
        for (int noise = 0; noise < numNoise; noise++) {
            dfNoise[noise] = in.readBit();
        }
    }

    void decodeInvF(BitStream in, FrequencyTables tables) throws AACException {
        final int numNoiseBands = tables.getNq();
        invfMode = new int[numNoiseBands];
        for (int i = 0; i < numNoiseBands; i++) {
            invfMode[i] = in.readBits(2);
        }
    }

    void copyInvF(ChannelData cd) {
        invfMode = new int[cd.invfMode.length];
        System.arraycopy(cd.invfMode, 0, invfMode, 0, invfMode.length);
    }

    void decodeEnvelope(BitStream in, FrequencyTables tables, boolean coupling) throws AACException {
        final int bits = 7 - ((coupling && (channel == 1)) ? 1 : 0) - ampRes;

        int delta;
        int[][] huffT, huffF;
        if (coupling && (channel == 1)) {
            delta = 2;
            if (ampRes == 1) {
                huffT = T_HUFFMAN_ENV_BAL_3_0DB;
                huffF = F_HUFFMAN_ENV_BAL_3_0DB;
            } else {
                huffT = T_HUFFMAN_ENV_BAL_1_5DB;
                huffF = F_HUFFMAN_ENV_BAL_1_5DB;
            }
        } else {
            delta = 1;
            if (ampRes == 1) {
                huffT = T_HUFFMAN_ENV_3_0DB;
                huffF = F_HUFFMAN_ENV_3_0DB;
            } else {
                huffT = T_HUFFMAN_ENV_1_5DB;
                huffF = F_HUFFMAN_ENV_1_5DB;
            }
        }

        int numEnvBands;
        dataEnv = new int[numEnv][];
        for (int l = 0; l < numEnv; l++) {
            numEnvBands = tables.getN()[freqRes[l]];
            dataEnv[l] = new int[numEnvBands];
            if (dfEnv[l] == 1) {
                for (int k = 0; k < numEnvBands; k++) {
                    dataEnv[l][k] = decodeHuffman(in, huffT);
                }
            } else {
                dataEnv[0][l] = in.readBits(bits);

                for (int k = 1; k < numEnvBands; k++) {
                    dataEnv[l][k] = decodeHuffman(in, huffF);
                }
            }
        }

        int sum, g;
        int[] ge;
        int[] fTableHigh = tables.getFTableHigh(), fTableLow = tables.getFTableLow();
        envelopeScalefactors = new int[numEnv][];
        for (int l = 0; l < numEnv; l++) {
            numEnvBands = tables.getN()[freqRes[l]];
            envelopeScalefactors[l] = new int[numEnvBands];

            if (dfEnv[l] == 0) {
                for (int k = 0; k < numEnvBands; k++) {
                    sum = 0;
                    for (int i = 0; i <= k; i++) {
                        sum += delta * dataEnv[l][i];
                    }
                    envelopeScalefactors[l][k] = sum;
                }
            } else {
                g = (l == 0) ? freqResPrev : freqRes[l - 1];
                ge = (l == 0) ? envelopeScalefactorsPrev : envelopeScalefactors[l - 1];
                int i;
                if (freqRes[l] == g) {
                    for (int k = 0; k < numEnvBands; k++) {
                        envelopeScalefactors[l][k] = ge[k] + delta * dataEnv[l][k];
                    }
                } else if (g == 1) {
                    for (int k = 0; k < numEnvBands; k++) {
                        i = -1;
                        for (int j = 0; j < fTableHigh.length && i < 0; j++) {
                            if (fTableHigh[j] == fTableLow[k]) i = j;
                        }
                        envelopeScalefactors[l][k] = ge[i] + delta * dataEnv[l][k];
                    }
                } else {
                    for (int k = 0; k < numEnvBands; k++) {
                        i = -1;
                        for (int j = 0; j < fTableLow.length && i < 0; j++) {
                            if (fTableLow[j] > fTableHigh[k]) i = j - 1;
                        }
                        envelopeScalefactors[l][k] = ge[i] + delta * dataEnv[l][k];
                    }
                }
            }
        }

        freqResPrev = freqRes[numEnv - 1];
        envelopeScalefactorsPrev = new int[envelopeScalefactors[numEnv - 1].length];
        System.arraycopy(envelopeScalefactors[numEnv - 1], 0, envelopeScalefactorsPrev, 0, envelopeScalefactorsPrev.length);
    }

    void decodeNoise(BitStream in, FrequencyTables tables, boolean coupling) throws AACException {
        int delta;
        int[][] huffT, huffF;
        if (coupling && (channel == 1)) {
            delta = 2;
            huffT = T_HUFFMAN_NOISE_BAL_3_0DB;
            huffF = F_HUFFMAN_ENV_BAL_3_0DB;
        } else {
            delta = 1;
            huffT = T_HUFFMAN_NOISE_3_0DB;
            huffF = F_HUFFMAN_ENV_3_0DB;
        }

        final int len = tables.getNq();
        int j;
        dataNoise = new int[numNoise][len];
        for (int i = 0; i < numNoise; i++) {
            if (dfNoise[i] == 1) {
                for (j = 0; j < len; j++) {
                    dataNoise[i][j] = decodeHuffman(in, huffT);
                }
            } else {
                dataNoise[0][i] = in.readBits(5);
                for (j = 1; j < len; j++) {
                    dataNoise[i][j] = decodeHuffman(in, huffF);
                }
            }
        }

        final int nq = tables.getNq();
        noiseFloorData = new int[numNoise][nq];
        int sum;
        int[] prev;
        for (int l = 0; l < numNoise; l++) {
            if (dfNoise[l] == 0) {
                for (int k = 0; k < nq; k++) {
                    sum = 0;
                    for (int i = 0; i <= k; i++) {
                        sum += delta * dataNoise[l][i];
                    }
                    noiseFloorData[l][k] = sum;
                }
            } else {
                prev = (l == 0) ? noiseFloorDataPrev : noiseFloorData[l - 1];
                for (int k = 0; k < nq; k++) {
                    noiseFloorData[l][k] = prev[k] + delta * dataNoise[l][k];
                }
            }
        }

        noiseFloorDataPrev = new int[noiseFloorData[numNoise - 1].length];
        System.arraycopy(noiseFloorData[numNoise - 1], 0, noiseFloorDataPrev, 0, noiseFloorDataPrev.length);
    }

    void decodeSinusoidals(BitStream in, FrequencyTables tables) throws AACException {
        addHarmonics = new int[tables.getNHigh()];

        harmonicsPresent = in.readBool();
        if (harmonicsPresent) {
            for (int n = 0; n < addHarmonics.length; n++) {
                addHarmonics[n] = in.readBit();
            }
        } else Arrays.fill(addHarmonics, 0);
    }

    private int decodeHuffman(BitStream in, int[][] table) throws AACException {
        int index = 0;
        int bit;
        while (index >= 0) {
            bit = in.readBit();
            index = table[index][bit];
        }
        return index + HUFFMAN_OFFSET;
    }

    int getChannelNumber() {
        return channel;
    }

    int getAmpRes() {
        return ampRes;
    }

    int[][] getEnvelopeScalefactors() {
        return envelopeScalefactors;
    }

    int[][] getNoiseFloorData() {
        return noiseFloorData;
    }

    int getNumEnv() {
        return numEnv;
    }

    int getNumEnvPrev() {
        return numEnvPrev;
    }

    int getNumNoise() {
        return numNoise;
    }

    int[] getFreqRes() {
        return freqRes;
    }

    int[] getTe() {
        return te;
    }

    int[] getTq() {
        return tq;
    }

    int[] getTePrev() {
        return tePrev;
    }

    int[] getInvfMode() {
        return invfMode;
    }

    int[] getInvfModePrev() {
        return invfModePrev;
    }

    float[][] getEorig() {
        return Eorig;
    }

    float[][] getQorig() {
        return Qorig;
    }

    void setDequantData(float[][] Eorig, float[][] Qorig) {
        this.Eorig = Eorig;
        this.Qorig = Qorig;
    }

    float[] getBwArray() {
        return bwArray;
    }

    float[] getBwArrayPrev() {
        return bwArrayPrev;
    }

    void setBwArray(float[] bwArray) {
        this.bwArray = bwArray;
    }

    int getLTemp() {
        return lTemp;
    }

    boolean areHarmonicsPresent() {
        return harmonicsPresent;
    }

    int[] getAddHarmonics() {
        return addHarmonics;
    }

    int getLa() {
        return La;
    }

    int getLaPrev() {
        return LaPrev;
    }

    int[] getSIndexMappedPrev() {
        return sIndexMappedPrev;
    }

    void setSIndexMappedPrev(int[] sIndexMappedPrev) {
        this.sIndexMappedPrev = sIndexMappedPrev;
    }

    void savePreviousData() {
        lTemp = te[numEnv] * RATE - TIME_SLOTS[0] * RATE;

        numEnvPrev = numEnv;
        tePrev = te;
        invfModePrev = invfMode;
        bwArrayPrev = bwArray;
        LaPrev = La;
    }
}

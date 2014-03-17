package net.sourceforge.jaad.aac.sbr;

class HFGenerator implements Constants {

    private static final float E_INV = 1E-6f;

    static void process(FrequencyTables tables, ChannelData cd, float[][][] Xlow, float[][][] Xhigh) {
        float[] alpha0 = new float[2];
        float[] alpha1 = new float[2];
        calculateAlpha(tables, alpha0, alpha1, Xlow);

        float[] bwArray = new float[tables.getNq()];
        calculateChirpFactors(tables, cd, bwArray);
        cd.setBwArray(bwArray);

        int[] te = cd.getTe();
        int Le = cd.getNumEnv();
        int kx = tables.getKx();
        int numPatches = tables.getNumPatches();
        int[] patchNumSubbands = tables.getPatchNumSubbands();
        int[] patchStartSubband = tables.getPatchStartSubband();
        int[] fTableNoise = tables.getFTableNoise();

        int k, p, g;
        float re1, re2, im1, im2, tmp;
        for (int l = RATE * te[0]; l < RATE * te[Le]; l++) {
            for (int i = 0; i < numPatches; i++) {
                for (int x = 0; x < patchNumSubbands[i]; x++) {
                    k = kx + x;
                    for (int q = 0; q < i; q++) {
                        k += patchNumSubbands[q];
                    }

                    p = patchStartSubband[i] + x;

                    g = -1;
                    for (int z = 0; z < fTableNoise.length && g < 0; z++) {
                        if (fTableNoise[z] <= k && fTableNoise[z + 1] > k) g = z;
                    }

                    //alpha0 * Xlow[p][l-1+T_HF_ADJ]
                    re1 = (alpha0[0] * Xlow[p][l - 1 + T_HF_ADJ][0]) - (alpha0[1] * Xlow[p][l - 1 + T_HF_ADJ][1]);
                    im1 = (alpha0[0] * Xlow[p][l - 1 + T_HF_ADJ][1]) + (alpha0[1] * Xlow[p][l - 1 + T_HF_ADJ][0]);
                    re1 *= bwArray[g];
                    im1 *= bwArray[g];

                    //alpha1 * Xlow[p][l-2+T_HF_ADJ]
                    re2 = (alpha1[0] * Xlow[p][l - 2 + T_HF_ADJ][0]) - (alpha1[1] * Xlow[p][l - 2 + T_HF_ADJ][1]);
                    im2 = (alpha1[0] * Xlow[p][l - 2 + T_HF_ADJ][1]) + (alpha1[1] * Xlow[p][l - 2 + T_HF_ADJ][0]);
                    tmp = bwArray[g] * bwArray[g];
                    re2 *= tmp;
                    im2 *= tmp;

                    Xhigh[k][l + T_HF_ADJ][0] = Xlow[p][l + T_HF_ADJ][0] + re1 + re2;
                    Xhigh[k][l + T_HF_ADJ][1] = Xlow[p][l + T_HF_ADJ][1] + im1 + im2;
                }
            }
        }
    }

    private static void calculateAlpha(FrequencyTables tables, float[] alpha0, float[] alpha1, float[][][] Xlow) {
        int k0 = tables.getK0();

        float[][][] phi = new float[3][3][2];
        float re, im;
        for (int i = 0; i < 3; i++) {
            for (int j = 1; j < 3; j++) {
                re = 0;
                im = 0;
                for (int k = 0; k < k0; k++) {
                    for (int n = 0; n < TIME_SLOTS[0] * RATE + 6 - 1; n++) {
                        //Xlow[k][n-i+T_HF_ADJ] * Xlow*[k][n-j+T_HF_ADJ]
                        re += (Xlow[k][n - i + T_HF_ADJ][0] * Xlow[k][n - j + T_HF_ADJ][0]) - (Xlow[k][n - i + T_HF_ADJ][1] * (-Xlow[k][n - j + T_HF_ADJ][0]));
                        im += (Xlow[k][n - i + T_HF_ADJ][0] * (-Xlow[k][n - j + T_HF_ADJ][1])) + (Xlow[k][n - i + T_HF_ADJ][1] * Xlow[k][n - j + T_HF_ADJ][0]);
                    }
                }
                phi[i][j][0] = re;
                phi[i][j][1] = im;
            }
        }

        re = (phi[2][2][0] * phi[1][1][0]) - (phi[2][2][1] * phi[1][1][1]);
        im = (phi[2][2][0] * phi[1][1][1]) + (phi[2][2][1] * phi[1][1][0]);
        float eInv = 1 / (1 + E_INV);
        float tmp = (phi[1][2][0] * phi[1][2][0]) + (phi[1][2][1] * phi[1][2][1]);
        float[] d = new float[]{re - tmp * eInv, im};

        if (d[0] == 0 && d[1] == 0) {
            alpha1[0] = 0;
            alpha1[1] = 0;
        } else {
            re = (phi[0][1][0] * phi[1][2][0]) - (phi[0][1][1] * phi[1][2][1]);
            im = (phi[0][1][0] * phi[1][2][1]) + (phi[0][1][1] * phi[1][2][0]);
            re -= (phi[0][2][0] * phi[1][1][0]) - (phi[0][2][1] * phi[1][1][1]);
            im -= (phi[0][2][0] * phi[1][1][1]) + (phi[0][2][1] * phi[1][1][0]);
            tmp = (d[0] * d[0]) + (d[1] * d[1]);
            alpha1[0] = ((re * d[0]) + (im * d[1])) / tmp;
            alpha1[1] = ((im * d[0]) - (re * d[1])) / tmp;
        }

        if (phi[1][1][0] == 0 && phi[1][1][1] == 0) {
            alpha0[0] = 0;
            alpha0[1] = 0;
        } else {
            re = (alpha1[0] * phi[1][2][0]) - (alpha1[1] * (-phi[1][2][1]));
            im = (alpha1[0] * (-phi[1][2][1])) + (alpha1[1] * phi[1][2][0]);
            re = phi[0][1][0] + re;
            im = phi[0][1][1] + im;
            tmp = (phi[1][1][0] * phi[1][1][0]) + (phi[1][1][1] * phi[1][1][1]);
            alpha0[0] = ((re * phi[1][1][0]) + (im * phi[1][1][1])) / tmp;
            alpha1[1] = ((im * phi[1][1][0]) - (re * phi[1][1][1])) / tmp;
        }

        //set both to zero if magnitude of one alpha is >= 4
        if ((alpha0[0] * alpha0[0] + alpha0[1] * alpha0[1]) >= 4
                || (alpha1[0] * alpha1[0] + alpha1[1] * alpha1[1]) >= 4) {
            alpha0[0] = 0;
            alpha0[1] = 0;
            alpha1[0] = 0;
            alpha1[1] = 0;
        }
    }

    private static void calculateChirpFactors(FrequencyTables tables, ChannelData cd, float[] bwArray) {
        int Nq = tables.getNq();
        int[] invfMode = cd.getInvfMode();
        int[] invfModePrev = cd.getInvfModePrev();
        float[] bwArrayPrev = cd.getBwArrayPrev();

        float newBw, tempBw;
        for (int i = 0; i < Nq; i++) {
            newBw = getNewBW(invfMode[i], invfModePrev[i]);
            if (newBw < bwArrayPrev[i]) tempBw = 0.75f * newBw + 0.25f * bwArrayPrev[i];
            else tempBw = 0.90625f * newBw + 0.09375f * bwArrayPrev[i];
            if (tempBw < 0.015625) bwArray[i] = 0;
            else bwArray[i] = tempBw;
        }
    }

    private static float getNewBW(int invfMode, int invfModePrev) {
        if (invfMode == 3) return 0.98f;
        else if (invfMode == 2) return 0.9f;
        else if (invfMode == 1 && invfModePrev != 0) return 0.75f;
        else if (invfMode == 0 && invfModePrev != 1) return 0;
        else return 0.6f;
    }
}

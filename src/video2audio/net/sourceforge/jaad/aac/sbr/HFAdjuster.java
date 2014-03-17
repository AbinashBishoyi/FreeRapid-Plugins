package net.sourceforge.jaad.aac.sbr;

class HFAdjuster implements Constants, NoiseTable {

    private static final float EPSILON_0 = 1e-12f;
    private static final float[] LIMITER_GAINS = {0.70795f, 1.0f, 1.41254f, 10000000000f};
    private static final float MAX_BOOST = 1.584893192f;
    private static final float[] H_SMOOTH = {0.33333333333333f,
            0.30150283239582f,
            0.21816949906249f,
            0.11516383427084f,
            0.03183050093751f
    };
    private float[][] EorigMapped, Qmapped;
    private int[][] sIndexMapped, sMapped;
    private float[][] Ecurr, Qm, Sm;
    private float[][] G, Gmax, QmLim, Glim;
    private float[][] Gboost;
    private float[][] GlimBoost, QmLimBoost, SmBoost;
    private float[][][] Gtemp, Qtemp;
    private int[] indexNoise, indexSine;

    HFAdjuster() {
        indexNoise = new int[2];
        indexSine = new int[2];
    }

    void process(Header header, FrequencyTables tables, ChannelData cd, float[][][] Xhigh, float[][][] Y) {
        map(tables, cd);
        int[] sIndexMappedPrev = new int[tables.getNHigh()];
        for (int i = 0; i < tables.getNHigh(); i++) {
            sIndexMappedPrev[i] = sIndexMapped[i][cd.getNumEnv() - 1];
        }
        cd.setSIndexMappedPrev(sIndexMappedPrev);

        estimateCurrentEnvelope(header, tables, cd, Xhigh);
        calculateAdditionalSignals(tables, cd);
        calculateGain(header, tables, cd);
        assembleSignals(header, tables, cd, Xhigh, Y);
    }

    private void map(FrequencyTables tables, ChannelData cd) {
        //mapping
        int Le = cd.getNumEnv();
        int[] r = cd.getFreqRes();
        int[] te = cd.getTe();
        int[] tq = cd.getTq();
        int[] n = tables.getN();
        int nHigh = tables.getNHigh(), nLow = tables.getNLow();
        int kx = tables.getKx();
        int[] fTableHigh = tables.getFTableHigh();
        int[] fTableLow = tables.getFTableLow();
        int Nq = tables.getNq();
        int[] fTableNoise = tables.getFTableNoise();

        //envelopes
        float[][] Eorig = cd.getEorig();
        int len = Math.max(fTableLow[nLow], fTableHigh[nHigh]);
        EorigMapped = new float[len - kx][Le];

        int[] fTable;
        for (int l = 0; l < Le; l++) {
            fTable = (r[l] == 1) ? fTableHigh : fTableLow;
            for (int i = 0; i < n[r[l]]; i++) {
                for (int m = fTable[i]; m < fTable[i + 1]; m++) {
                    EorigMapped[m - kx][l] = Eorig[i][l];
                }
            }
        }

        //noise
        float[][] Qorig = cd.getQorig();
        Qmapped = new float[fTableNoise[Nq] - kx][Le];
        int k;
        for (int l = 0; l < Le; l++) {
            k = -1;
            for (int z = 0; z < tq.length && k < 0; z++) {
                if ((RATE * te[l] >= RATE * tq[z]) && (RATE * te[l + 1] <= RATE * tq[z + 1])) k = z;
            }
            for (int i = 0; i < Nq; i++) {
                for (int m = fTableNoise[i]; m < fTableNoise[i + 1]; m++) {
                    Qmapped[m - kx][l] = Qorig[i][k];
                }
            }
        }

        //sinusoids
        int[] sIndex = cd.getAddHarmonics();
        int La = cd.getLa();
        int[] sIndexMappedPrev = cd.getSIndexMappedPrev();

        sIndexMapped = new int[fTableHigh[nHigh] - kx][Le];
        int tmp, delta;
        for (int l = 0; l < Le; l++) {
            for (int i = 0; i < nHigh; i++) {
                for (int m = fTableHigh[i]; m < fTableHigh[i + 1]; m++) {
                    tmp = (int) ((float) (fTableHigh[i + 1] + fTableHigh[i]) / 2.0f);
                    if (m == tmp) {
                        delta = (l >= La || sIndexMappedPrev[m - kx] == 1) ? 1 : 0;
                        sIndexMapped[m - kx][l] = sIndex[i] * delta;
                    } else sIndexMapped[m - kx][l] = 0;
                }
            }
        }

        sMapped = new int[len - kx][Le];
        for (int l = 0; l < Le; l++) {
            fTable = (r[l] == 1) ? fTableHigh : fTableLow;
            for (int i = 0; i < n[r[l]]; i++) {
                delta = 0;
                for (int j = fTable[i]; j < fTable[i + 1] && delta == 0; j++) {
                    if (sIndexMapped[j - kx][l] == 1) delta = 1;
                }
                for (int m = fTable[i]; m < fTable[i + 1]; m++) {
                    sMapped[m - kx][l] = delta;
                }
            }
        }
    }

    private void estimateCurrentEnvelope(Header header, FrequencyTables tables, ChannelData cd, float[][][] Xhigh) {
        int Le = cd.getNumEnv();
        int[] te = cd.getTe();
        int[] r = cd.getFreqRes();
        int M = tables.getM();
        int kx = tables.getKx();
        int[] n = tables.getN();
        int[] fTableHigh = tables.getFTableHigh(), fTableLow = tables.getFTableLow();

        float sum, f;
        if (header.getInterpolFreq() == 1) {
            Ecurr = new float[M][Le];
            for (int l = 0; l < Le; l++) {
                f = RATE * te[l + 1] - RATE * te[l];
                for (int m = 0; m < M; m++) {
                    sum = 0;
                    for (int i = RATE * te[l] + T_HF_ADJ; i <= RATE * te[l + 1] - 1 + T_HF_ADJ; i++) {
                        sum += Xhigh[m + kx][l][0] * Xhigh[m + kx][l][0] + Xhigh[m + kx][l][1] * Xhigh[m + kx][l][1];
                    }
                    Ecurr[m][l] = sum / f;
                }
            }
        } else {
            int len = Math.max(fTableHigh[n[1]], fTableLow[n[0]]);
            Ecurr = new float[len][Le];
            int[] fTable;
            int kl, kh;
            for (int l = 0; l < Le; l++) {
                fTable = (r[l] == 1) ? fTableHigh : fTableLow;
                for (int p = 0; p < n[r[l]]; p++) {
                    kl = fTable[p];
                    kh = fTable[p + 1] - 1;
                    f = (RATE * te[l + 1] - RATE * te[l]) * (kh - kl + 1);

                    for (int k = kl; k <= kh; k++) {
                        sum = 0;
                        for (int i = RATE * te[l] + T_HF_ADJ; i <= RATE * te[l + 1] - 1 + T_HF_ADJ; i++) {
                            for (int j = kl; j <= kh; j++) {
                                sum += Xhigh[j][i][0] * Xhigh[j][i][0] + Xhigh[j][i][1] * Xhigh[j][i][1];
                            }
                        }
                        Ecurr[k - kx][l] = sum / f;
                    }
                }
            }
        }
    }

    private void calculateAdditionalSignals(FrequencyTables tables, ChannelData cd) {
        int Le = cd.getNumEnv();
        int M = tables.getM();

        float f;
        Qm = new float[M][Le];
        for (int l = 0; l < Le; l++) {
            for (int m = 0; m < M; m++) {
                f = Qmapped[m][l] / (1 + Qmapped[m][l]);
                Qm[m][l] = (float) Math.sqrt(EorigMapped[m][l] * f);
            }
        }

        Sm = new float[M][Le];
        for (int l = 0; l < Le; l++) {
            for (int m = 0; m < M; m++) {
                f = sIndexMapped[m][l] / (1 + Qmapped[m][l]);
                Sm[m][l] = (float) Math.sqrt(EorigMapped[m][l] * f);
            }
        }
    }

    private void calculateGain(Header header, FrequencyTables tables, ChannelData cd) {
        int Le = cd.getNumEnv();
        int La = cd.getLa();
        int lap = (cd.getLaPrev() == cd.getNumEnvPrev()) ? 0 : -1;
        int M = tables.getM();
        int Nl = tables.getNl();
        int[] fTableLim = tables.getFTableLim();
        int kx = tables.getKx();

        G = new float[M][Le];
        int delta;
        float f;
        for (int l = 0; l < Le; l++) {
            for (int m = 0; m < M; m++) {
                delta = (l == La || l == lap) ? 0 : 1;
                if (sMapped[m][l] == 0) f = EorigMapped[m][l] / ((1 + Ecurr[m][l]) * (1 + delta * Qmapped[m][l]));
                else f = (EorigMapped[m][l] / (1 + Ecurr[m][l])) * (Qmapped[m][l] / (1 + Qmapped[m][l]));
                G[m][l] = (float) Math.sqrt(f);
            }
        }

        float[][] GmaxTemp = new float[Nl][Le];
        float f2;
        float limGain = LIMITER_GAINS[header.getLimiterGains()];
        for (int l = 0; l < Le; l++) {
            for (int k = 0; k < Nl; k++) {
                f = EPSILON_0;
                f2 = EPSILON_0;
                for (int i = fTableLim[k] - kx; i < fTableLim[k + 1] - kx; i++) {
                    f += EorigMapped[i][l];
                    f2 += Ecurr[i][l];
                }
                GmaxTemp[k][l] = (float) Math.sqrt(f / f2) * limGain;
            }
        }

        Gmax = new float[M][Le];
        int z;
        for (int l = 0; l < Le; l++) {
            for (int m = 0; m < M; m++) {
                z = -1;
                for (int i = 0; i < fTableLim.length && z < 0; i++) {
                    if ((fTableLim[i] <= m + kx)
                            && (i == fTableLim.length - 1 || (fTableLim[i + 1] > m + kx))) z = i;
                }
                Gmax[m][l] = Math.min(GmaxTemp[z][l], 100000);
            }
        }

        QmLim = new float[M][Le];
        Glim = new float[M][Le];
        for (int l = 0; l < Le; l++) {
            for (int m = 0; m < M; m++) {
                QmLim[m][l] = Math.min(Qm[m][l], Qm[m][l] * (Gmax[m][l] / G[m][l]));
                Glim[m][l] = Math.min(G[m][l], Gmax[m][l]);
            }
        }

        float[][] GboostTemp = new float[Nl][Le];
        for (int l = 0; l < Le; l++) {
            for (int k = 0; k < Nl; k++) {
                f = EPSILON_0;
                f2 = EPSILON_0;
                for (int i = fTableLim[k] - kx; i < fTableLim[k + 1] - kx; i++) {
                    f += EorigMapped[i][l];
                    delta = (Sm[i][l] != 0 || l == La || l == lap) ? 0 : 1;
                    f2 += (Ecurr[i][l] * Glim[i][l] * Glim[i][l])
                            + (Sm[i][l] * Sm[i][l])
                            + (delta * QmLim[i][l] * QmLim[i][l]);
                }
                GboostTemp[k][l] = (float) Math.sqrt(f / f2);
            }
        }

        Gboost = new float[M][Le];
        for (int l = 0; l < Le; l++) {
            for (int m = 0; m < M; m++) {
                z = -1;
                for (int i = 0; i < fTableLim.length - 1 && z < 0; i++) {
                    if ((fTableLim[i] <= m + kx) && (fTableLim[i + 1] > m + kx)) z = i;
                }
                Gboost[m][l] = Math.min(GboostTemp[z][l], MAX_BOOST);
            }
        }

        GlimBoost = new float[M][Le];
        QmLimBoost = new float[M][Le];
        SmBoost = new float[M][Le];
        for (int l = 0; l < Le; l++) {
            for (int m = 0; m < M; m++) {
                GlimBoost[m][l] = Glim[m][l] * Gboost[m][l];
                QmLimBoost[m][l] = QmLim[m][l] * Gboost[m][l];
                SmBoost[m][l] = Sm[m][l] * Gboost[m][l];
            }
        }
    }

    private void assembleSignals(Header header, FrequencyTables tables, ChannelData cd, float[][][] Xhigh, float[][][] Y) {
        int ch = cd.getChannelNumber();
        int Le = cd.getNumEnv();
        int[] te = cd.getTe();
        int La = cd.getLa();
        int lap = (cd.getLaPrev() == cd.getNumEnvPrev()) ? 0 : -1;
        int M = tables.getM();
        int kx = tables.getKx();
        int hSL = header.getSmoothingMode() == 0 ? 4 : 0;

        //gtemp of previous frame
        if (Gtemp == null) Gtemp = new float[2][M][50]; //TODO: len: RATE*te[Le]+4
        if (header.isReset()) {
            for (int l = 0; l < Le; l++) {
                for (int m = 0; m < M; m++) {
                    for (int i = 0; i < hSL; i++) {
                        Gtemp[ch][m][i] = GlimBoost[m][0];
                    }
                }
            }
        } else if (hSL != 0) {
            for (int l = 0; l < Le; l++) {
                for (int m = 0; m < M; m++) {
                    for (int i = 0; i < hSL; i++) {
                        Gtemp[ch][m][i] = Gtemp[ch][m][Gtemp[ch][m].length - 4 + i];
                    }
                }
            }
        }

        //gtemp -> gfilt -> W1
        for (int l = 0; l < Le; l++) {
            for (int m = 0; m < M; m++) {
                for (int i = RATE * te[l]; i < RATE * te[l + 1]; i++) {
                    try {
                        Gtemp[ch][m][i + hSL] = GlimBoost[m][l];
                    } catch (ArrayIndexOutOfBoundsException e) {
                        System.out.println("te[l+1]: " + te[l + 1] + ", te[Le]: " + te[Le]);
                        System.out.println("TIME_SLOTS[0]: " + TIME_SLOTS[0]);
                        System.out.println("m: " + m + " (" + M + ")");
                        System.out.println("l: " + l + " (" + Le + ")");
                        System.out.println("i: " + i + " (" + RATE * te[l + 1] + ")");
                        System.out.println("Gtemp[ch]: " + Gtemp[ch].length + ", " + Gtemp[ch][0].length);
                        System.out.println("GlimBoost: " + GlimBoost.length + ", " + GlimBoost[0].length);
                        throw e;
                    }
                }
            }
        }
        float[][] Gfilt = new float[M][50]; //TODO: len: RATE*te[Le]
        boolean b;
        float sum;
        for (int l = 0; l < Le; l++) {
            b = l != La && l != lap && hSL != 0;
            for (int m = 0; m < M; m++) {
                for (int i = RATE * te[l]; i < RATE * te[l + 1]; i++) {
                    if (b) {
                        sum = 0;
                        for (int j = 0; j < hSL; j++) {
                            sum += Gtemp[ch][m][i - j + hSL] * H_SMOOTH[j];
                        }
                        Gfilt[m][i] = sum;
                    } else Gfilt[m][i] = Gtemp[ch][m][i + hSL];
                }
            }
        }

        float[][][] W1 = new float[M][50][2]; //TODO: len: RATE*te[Le]
        for (int m = 0; m < M; m++) {
            for (int i = RATE * te[0]; i < RATE * te[Le]; i++) {
                W1[m][i][0] = Gfilt[m][i] * Xhigh[m + kx][i + T_HF_ADJ][0];
                W1[m][i][1] = Gfilt[m][i] * Xhigh[m + kx][i + T_HF_ADJ][1];
            }
        }

        //qtemp of previous frame
        if (Qtemp == null) Qtemp = new float[2][M][50]; //TODO: len: RATE*TIME_SLOTS[0]+hSL
        if (header.isReset()) {
            for (int l = 0; l < Le; l++) {
                for (int m = 0; m < M; m++) {
                    for (int i = 0; i < hSL; i++) {
                        Qtemp[ch][m][i] = QmLimBoost[m][0];
                    }
                }
            }
        } else if (hSL != 0) {
            for (int l = 0; l < Le; l++) {
                for (int m = 0; m < M; m++) {
                    for (int i = 0; i < hSL; i++) {
                        Qtemp[ch][m][i] = Qtemp[ch][m][Qtemp[ch][m].length - 4 + i];
                    }
                }
            }
        }

        //qtemp -> qfilt -> W2
        for (int l = 0; l < Le; l++) {
            for (int m = 0; m < M; m++) {
                for (int i = RATE * te[l]; i < RATE * te[l + 1]; i++) {
                    try {
                        Qtemp[ch][m][i + hSL] = QmLimBoost[m][l];
                    } catch (ArrayIndexOutOfBoundsException e) {
                        System.out.println("te[l+1]: " + te[l + 1] + ", te[Le]: " + te[Le]);
                        System.out.println("TIME_SLOTS[0]: " + TIME_SLOTS[0]);
                        System.out.println("m: " + m + " (" + M + ")");
                        System.out.println("l: " + l + " (" + Le + ")");
                        System.out.println("i: " + i + " (" + RATE * te[l + 1] + ")");
                        System.out.println("Qtemp[ch]: " + Qtemp[ch].length + ", " + Qtemp[ch][0].length);
                        System.out.println("QmLimBoost: " + QmLimBoost.length + ", " + QmLimBoost[0].length);
                        throw e;
                    }
                }
            }
        }

        float[][] Qfilt = new float[M][50]; //TODO: len: RATE*TIME_SLOTS[0]
        for (int l = 0; l < Le; l++) {
            for (int m = 0; m < M; m++) {
                b = l != La && l != lap && SmBoost[m][l] == 0;
                for (int i = RATE * te[l]; i < RATE * te[l + 1]; i++) {
                    if (b && hSL == 0) Qfilt[m][i] = Qtemp[ch][m][i];
                    else if (b && hSL != 0) {
                        sum = 0;
                        for (int j = 0; j < hSL; j++) {
                            sum += Qtemp[ch][m][i - j + hSL] * H_SMOOTH[j];
                        }
                        Qfilt[m][i] = sum;
                    } else Qfilt[m][i] = 0;
                }
            }
        }

        float[][][] W2 = new float[M][50][2]; //TODO: len: RATE*TIME_SLOTS[0]
        int indNoise = header.isReset() ? 0 : indexNoise[ch];
        int index = 0;
        for (int l = 0; l < Le; l++) {
            for (int m = 0; m < M; m++) {
                for (int i = RATE * te[l]; i < RATE * te[l + 1]; i++) {
                    index = (indNoise + (i - RATE * te[0]) * M + m + 1) % 512;
                    W2[m][i][0] = W1[m][i][0] + (Qfilt[m][i] * NOISE_TABLE[index][0]);
                    W2[m][i][1] = W1[m][i][1] + (Qfilt[m][i] * NOISE_TABLE[index][1]);
                }
            }
        }
        indexNoise[ch] = index;

        //W1,W2,sinusoids -> Y
        int indSine = (indexSine[ch] + 1) % 4;
        float sign;
        int[] phiReSin = {1, 0, -1, 0};
        int[] phiImSin = {0, 1, 0, -1};
        for (int l = 0; l < Le; l++) {
            for (int m = 0; m < M; m++) {
                sign = (float) Math.pow(-1, m + kx);
                for (int i = RATE * te[l]; i < RATE * te[l + 1]; i++) {
                    index = (indSine + i - RATE * te[0]) % 4;
                    Y[m + kx][i + T_HF_ADJ][0] = W2[m][i][0] + (SmBoost[m][l] * phiReSin[index]);
                    Y[m + kx][i + T_HF_ADJ][1] = W2[m][i][1] + (SmBoost[m][l] * sign * phiImSin[index]);
                }
            }
        }
        indexSine[ch] = index;
    }
}

package net.sourceforge.jaad.aac.sbr;

import net.sourceforge.jaad.aac.AACException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class FrequencyTables implements Constants {

    private final int sampleFrequency;
    private int k0, k2, nMaster, M, kx, Nq, nLow, nHigh, Nl;
    private int Mprev, kxPrev;
    private int[] fMaster, nTable, fTableHigh, fTableLow, fTableNoise, fTableLim;
    private int numPatches;
    private int[] patchNumSubbands, patchStartSubband;
    private int[] patchBorders;

    FrequencyTables(int sampleFrequency) {
        this.sampleFrequency = sampleFrequency;
        kx = 0; //=kxPrev for first frame
        M = 0; //=Mprev for first frame
    }

    void calculateTables(Header header) throws AACException {
        kxPrev = kx;
        Mprev = M;

        calculateKs(header);
        int maxBands;
        if (sampleFrequency <= 32000) maxBands = 48;
        else if (sampleFrequency >= 48000) maxBands = 32;
        else maxBands = 35;
        if ((k2 - k0) > maxBands) throw new AACException("SBR: too many QMF bands: " + (k2 - k0));

        if (header.getFreqScale() == 0) calculateMasterTableFS0(header);
        else calculateMasterTableFS(header);
        if (nMaster < header.getXOverBand())
            throw new AACException("SBR: MFT length < xOver-band: " + nMaster + " < " + header.getXOverBand());

        calculateDerivedFrequencyTables(header);
        if (kx > 32) throw new AACException("SBR: start frequency border out of range");
        if ((kx + M) > 64) throw new AACException("SBR: stop frequency border out of range");
        if (Nq > 5) throw new AACException("SBR: too many noise floor scalefactors: " + Nq);

        calculatePatches();
        calculateLimiterFrequencyTables(header);
    }

    private void calculateKs(Header header) {
        int startMin;
        if (sampleFrequency < 32000) startMin = 3000;
        else if (sampleFrequency >= 64000) startMin = 5000;
        else startMin = 4000;
        startMin = (int) Math.round(startMin * (128.0 / (double) sampleFrequency));

        int index;
        if (sampleFrequency == 16000) index = 0;
        else if (sampleFrequency == 22050) index = 1;
        else if (sampleFrequency == 24000) index = 2;
        else if (sampleFrequency == 32000) index = 3;
        else if (sampleFrequency >= 44100 && sampleFrequency <= 64000) index = 4;
        else index = 5;
        k0 = startMin + OFFSET[index][header.getStartFreq()];

        final int stopFreq = header.getStopFreq();
        int min;
        if (stopFreq == 14) min = 2 * k0;
        else if (stopFreq == 15) min = 3 * k0;
        else {
            int stopMin;
            if (sampleFrequency < 32000) stopMin = 6000;
            else if (sampleFrequency >= 32000 && sampleFrequency < 64000) stopMin = 8000;
            else stopMin = 10000;
            stopMin = (int) Math.round(stopMin * (128.0 / (double) sampleFrequency));

            int[] stopDk = new int[13];
            double d = 64.0 / (double) stopMin;
            double expPrev = 0, exp;
            for (int p = 0; p < 13; p++) {
                exp = (double) (p + 1) / 13.0;
                stopDk[p] = (int) Math.round(stopMin * Math.pow(d, exp))
                        - (int) Math.round(stopMin * Math.pow(d, expPrev));
                expPrev = exp;
            }

            Arrays.sort(stopDk);

            int sum = 0;
            for (int i = 0; i <= header.getStopFreq() - 1; i++) {
                sum += stopDk[i];
            }
            min = stopMin + sum;
        }
        k2 = Math.min(64, min);
    }

    private void calculateMasterTableFS0(Header header) {
        int dk, numBands;
        if (header.getAlterScale() == 0) {
            dk = 1;
            numBands = 2 * (int) ((double) (k2 - k0) / 2.0);
        } else {
            dk = 2;
            numBands = 2 * (int) Math.round((double) (k2 - k0) / 4.0);
        }

        int k2Achieved = k0 + numBands * dk;
        int k2Diff = k2 - k2Achieved;
        int[] vDk = new int[numBands];
        for (int k = 0; k < numBands; k++) {
            vDk[k] = dk;
        }

        if (k2Diff != 0) {
            int incr, k;
            if (k2Diff < 0) {
                incr = 1;
                k = 0;
            } else {
                incr = -1;
                k = numBands - 1;
            }
            while (k2Diff != 0) {
                vDk[k] -= incr;
                k += incr;
                k2Diff += incr;
            }
        }

        fMaster = new int[numBands + 1];
        fMaster[0] = k0;
        for (int k = 1; k <= numBands; k++) {
            fMaster[k] = fMaster[k - 1] + vDk[k - 1];
        }
        nMaster = numBands;
    }

    private void calculateMasterTableFS(Header header) {
        int[] temp1 = {12, 10, 8};
        int bands = temp1[header.getFreqScale() - 1];
        double[] temp2 = {1.0, 1.3};
        double warp = temp2[header.getAlterScale()];

        boolean twoRegions;
        int k1;
        if (((double) k2 / (double) k0) > 2.2449) {
            twoRegions = true;
            k1 = 2 * k0;
        } else {
            twoRegions = false;
            k1 = k2;
        }

        int numBands0 = 2 * (int) Math.round(bands * Math.log((double) k1 / (double) k0) / (2 * Math.log(2)));
        int[] vDk0 = new int[numBands0];
        double d = (double) k1 / (double) k0;
        double expPrev = 0, exp;
        for (int k = 0; k < numBands0; k++) {
            exp = (double) (k + 1) / (double) numBands0;
            vDk0[k] = (int) Math.round((double) k0 * Math.pow(d, exp))
                    - (int) Math.round((double) k0 * Math.pow(d, expPrev));
            expPrev = exp;
        }

        Arrays.sort(vDk0);

        int[] vk0 = new int[numBands0 + 1];
        vk0[0] = k0;
        for (int k = 1; k <= numBands0; k++) {
            vk0[k] = vk0[k - 1] + vDk0[k - 1];
        }

        if (twoRegions) {
            d = (double) k2 / (double) k1;
            int numBands1 = 2 * (int) Math.round((double) bands * Math.log(d) / (2.0 * Math.log(2) * warp));
            int[] vDk1 = new int[numBands1];
            expPrev = 0;
            for (int k = 0; k < numBands1; k++) {
                exp = (double) (k + 1) / (double) numBands1;
                vDk1[k] = (int) Math.round((double) k1 * Math.pow(d, exp))
                        - (int) Math.round((double) k1 * Math.pow(d, expPrev));
                expPrev = exp;
            }

            if (min(vDk1) < max(vDk0)) {
                Arrays.sort(vDk1);
                int change = max(vDk0) - vDk1[0];
                int x = (int) (((double) vDk1[numBands1 - 1] - (double) vDk1[0]) / 2.0);
                if (change > x) change = x;
                vDk1[0] += change;
                vDk1[numBands1 - 1] -= change;
            }

            Arrays.sort(vDk1);
            int[] vk1 = new int[numBands1 + 1];
            vk1[0] = k1;
            for (int k = 1; k <= numBands1; k++) {
                vk1[k] = vk1[k - 1] + vDk1[k - 1];
            }

            nMaster = numBands0 + numBands1;
            fMaster = new int[nMaster + 1];
            for (int k = 0; k <= numBands0; k++) {
                fMaster[k] = vk0[k];
            }
            for (int k = numBands0 + 1; k <= nMaster; k++) {
                fMaster[k] = vk1[k - numBands0];
            }
        } else {
            nMaster = numBands0;
            fMaster = new int[numBands0 + 1];
            for (int k = 0; k <= numBands0; k++) {
                fMaster[k] = vk0[k];
            }
        }
    }

    private void calculateDerivedFrequencyTables(Header header) {
        nHigh = nMaster - header.getXOverBand();
        int x = (int) ((double) nHigh / 2.0);
        nLow = x + (nHigh - 2 * x);
        nTable = new int[]{nLow, nHigh};

        fTableHigh = new int[nHigh + 1];
        int xOver = header.getXOverBand();
        for (int k = 0; k <= nHigh; k++) {
            fTableHigh[k] = fMaster[k + xOver];
        }

        M = fTableHigh[nHigh] - fTableHigh[0];
        kx = fTableHigh[0];

        fTableLow = new int[nLow + 1];
        int i;
        for (int k = 0; k <= nLow; k++) {
            if (k == 0) i = 0;
            else i = 2 * k - ((nHigh % 2 == 0) ? 0 : 1);
            fTableLow[k] = fTableHigh[i];
        }

        double d = Math.log((double) k2 / (double) kx) / Math.log(2);
        int z = (int) Math.round((double) header.getNoiseBands() * d);
        Nq = Math.max(1, z);

        fTableNoise = new int[Nq + 1];
        fTableNoise[0] = fTableLow[0];
        i = 0;
        for (int k = 1; k <= Nq; k++) {
            i += (int) ((double) (nLow - i) / (double) (Nq + 1 - k));
            fTableNoise[k] = fTableLow[i];
        }
    }

    private void calculatePatches() {
        List<Integer> pns = new ArrayList<Integer>();
        List<Integer> pss = new ArrayList<Integer>();

        int msb = k0;
        int usb = kx;
        numPatches = 0;
        int goalSb = (int) Math.round(2.048E6f / (float) sampleFrequency);
        int k;
        if (goalSb < kx + M) {
            k = 0;
            for (int i = 0; fMaster[i] < goalSb; i++) {
                k = i + 1;
            }
        } else k = nMaster;

        int sb, odd, j;
        do {
            j = k;
            do {
                sb = fMaster[j];
                odd = (sb - 2 + k0) % 2;
                j--;
            }
            while (sb > (k0 - 1 + msb - odd));

            pns.add(numPatches, Math.max(sb - usb, 0));
            pss.add(numPatches, k0 - odd - pns.get(numPatches));

            if (pns.get(numPatches) > 0) {
                usb = sb;
                msb = sb;
                numPatches++;
            } else msb = kx;

            if (fMaster[k] - sb < 3) k = nMaster;
        }
        while (sb != kx + M);

        if (pns.get(numPatches - 1) < 3 && numPatches > 1) numPatches--;

        patchNumSubbands = new int[numPatches];
        for (int i = 0; i < numPatches; i++) {
            patchNumSubbands[i] = pns.get(i);
        }
        patchStartSubband = new int[numPatches];
        for (int i = 0; i < numPatches; i++) {
            patchStartSubband[i] = pss.get(i);
        }
    }

    private void calculateLimiterFrequencyTables(Header header) {
        int lb = header.getLimiterBands();
        if (lb == 0) {
            fTableLim = new int[]{fTableLow[0], fTableLow[nLow]};
            Nl = 1;
        } else {
            double[] limiterBandsPerOctave = {1.2, 2, 3};
            double limBands = limiterBandsPerOctave[lb - 1];

            patchBorders = new int[numPatches + 1];
            patchBorders[0] = kx;
            for (int k = 1; k <= numPatches; k++) {
                patchBorders[k] = patchBorders[k - 1] + patchNumSubbands[k - 1];
            }

            int[] limTable = new int[nLow + numPatches];
            for (int k = 0; k <= nLow; k++) {
                limTable[k] = fTableLow[k];
            }
            for (int k = 1; k < numPatches; k++) {
                limTable[k + nLow] = patchBorders[k];
            }

            Arrays.sort(limTable);
            List<Integer> limTableL = new ArrayList<Integer>();
            for (int i = 0; i < limTable.length; i++) {
                limTableL.add(limTable[i]);
            }

            int k = 1;
            int nrLim = nLow + numPatches - 1;
            while (k <= nrLim) {
                double nOctaves = Math.log((double) limTableL.get(k) / (double) limTableL.get(k - 1)) / Math.log(2);
                if (nOctaves * limBands < 0.49) {
                    if (limTableL.get(k) == limTableL.get(k - 1)) {
                        limTableL.remove(k);
                        nrLim--;
                    } else {
                        if (contains(patchBorders, limTableL.get(k))) {
                            if (contains(patchBorders, limTableL.get(k - 1))) k++;
                            else {
                                limTableL.remove(k - 1);
                                nrLim--;
                            }
                        } else {
                            limTableL.remove(k);
                            nrLim--;
                        }
                    }
                } else k++;
            }

            Nl = nrLim;
            fTableLim = new int[nrLim + 1];
            for (int i = 0; i <= nrLim; i++) {
                fTableLim[i] = limTableL.get(i);
            }
        }
    }

    private int min(int[] array) {
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < array.length; i++) {
            if (array[i] < min) min = array[i];
        }
        return min;
    }

    private int max(int[] array) {
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < array.length; i++) {
            if (array[i] > max) max = array[i];
        }
        return max;
    }

    private boolean contains(int[] array, int element) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == element) return true;
        }
        return false;
    }

    int getK0() {
        return k0;
    }

    int getK2() {
        return k2;
    }

    int getNMaster() {
        return nMaster;
    }

    int[] getFMaster() {
        return fMaster;
    }

    int[] getN() {
        return nTable;
    }

    int getNLow() {
        return nLow;
    }

    int getNHigh() {
        return nHigh;
    }

    int getM() {
        return M;
    }

    int getMPrev() {
        return Mprev;
    }

    int getKx() {
        return kx;
    }

    int getKxPrev() {
        return kxPrev;
    }

    int getNq() {
        return Nq;
    }

    int[] getFTableHigh() {
        return fTableHigh;
    }

    int[] getFTableLow() {
        return fTableLow;
    }

    int[] getFTableNoise() {
        return fTableNoise;
    }

    int[] getFTableLim() {
        return fTableLim;
    }

    int getNl() {
        return Nl;
    }

    int getNumPatches() {
        return numPatches;
    }

    int[] getPatchNumSubbands() {
        return patchNumSubbands;
    }

    int[] getPatchStartSubband() {
        return patchStartSubband;
    }

    int[] getPatchBorders() {
        return patchBorders;
    }
}

package net.sourceforge.jaad.aac.sbr;

import net.sourceforge.jaad.aac.AACException;
import net.sourceforge.jaad.aac.SampleFrequency;
import net.sourceforge.jaad.aac.syntax.BitStream;

public class SBR implements Constants {

    private final int sampleFrequency;
    private final boolean downSampled;
    private final Header header;
    private final FrequencyTables tables;
    private final AnalysisFilterbank analysisFilter;
    private final SynthesisFilterbank synthesisFilter;
    private final float[][][][] W, Y;
    private final float[][][] Xlow, Xhigh, X;
    private final HFAdjuster adjuster;
    private ChannelData channel1, channel2;
    private boolean coupling;

    public SBR(SampleFrequency sf, boolean downSampled) {
        sampleFrequency = sf.getFrequency() * 2;
        this.downSampled = downSampled;

        header = new Header();
        tables = new FrequencyTables(sampleFrequency);
        analysisFilter = new AnalysisFilterbank();
        synthesisFilter = new SynthesisFilterbank();
        W = new float[2][32][64][2];
        Xlow = new float[32][TIME_SLOTS[0] * RATE + T_HF_GEN][2];
        Xhigh = new float[64][TIME_SLOTS[0] * RATE + T_HF_GEN][2];
        Y = new float[2][64][TIME_SLOTS[0] * RATE + T_HF_GEN][2];
        X = new float[64][TIME_SLOTS[0] * RATE][2];

        adjuster = new HFAdjuster();

        channel1 = new ChannelData(0);
        coupling = false;
    }

    public void decode(BitStream in, int count, boolean stereo, boolean crc) throws AACException {
        final int start = in.getPosition();

        if (crc) in.skipBits(10); //TODO: CRC

        if (in.readBool()) {
            header.decode(in);
            if (header.isReset()) tables.calculateTables(header);
        }

        if (header.isInitialized()) {
            if (stereo) decodeChannelPair(in);
            else decodeSingleChannel(in);

            if (in.readBool()) {
                int size = in.readBits(4);
                if (size == 15) size += in.readBits(8);
                size *= 8;
                in.skipBits(size);

				/*int id;
                 while(size>7) {
				 id = in.readBits(2);
				 size -= 2;
				 decodeExtension(in, id);
				 }
				 in.readBits(size);*/
            }
        }

        int read = in.getPosition() - start;
        int left = count - read;
        if (left < 0) throw new AACException("SBR: bitstream overread: " + Math.abs(left));
        in.skipBits(left);
    }

    private void decodeSingleChannel(BitStream in) throws AACException {
        if (in.readBool()) in.skipBits(4); //reserved

        channel1.decodeGrid(in, header, tables);
        channel1.decodeDTDF(in);
        channel1.decodeInvF(in, tables);
        channel1.decodeEnvelope(in, tables, false);
        channel1.decodeNoise(in, tables, false);
        channel1.decodeSinusoidals(in, tables);

        dequantChannel(channel1);
    }

    private void decodeChannelPair(BitStream in) throws AACException {
        if (channel2 == null) channel2 = new ChannelData(1);

        if (in.readBool()) in.skipBits(8); //reserved

        coupling = in.readBool();
        if (coupling) {
            channel1.decodeGrid(in, header, tables);
            channel2.copyGrid(channel1);
            channel1.decodeDTDF(in);
            channel2.decodeDTDF(in);
            channel1.decodeInvF(in, tables);
            channel2.copyInvF(channel1);
            channel1.decodeEnvelope(in, tables, coupling);
            channel1.decodeNoise(in, tables, coupling);
            channel2.decodeEnvelope(in, tables, coupling);
            channel2.decodeNoise(in, tables, coupling);
        } else {
            channel1.decodeGrid(in, header, tables);
            channel2.decodeGrid(in, header, tables);
            channel1.decodeDTDF(in);
            channel2.decodeDTDF(in);
            channel1.decodeInvF(in, tables);
            channel2.decodeInvF(in, tables);
            channel1.decodeEnvelope(in, tables, coupling);
            channel2.decodeEnvelope(in, tables, coupling);
            channel1.decodeNoise(in, tables, coupling);
            channel2.decodeNoise(in, tables, coupling);
        }

        channel1.decodeSinusoidals(in, tables);
        channel2.decodeSinusoidals(in, tables);

        if (coupling) dequantCoupledChannels();
        else {
            dequantChannel(channel1);
            dequantChannel(channel2);
        }
    }

    private void decodeExtension(BitStream in, int id) throws AACException {
    }

    private void dequantChannel(ChannelData cd) {
        //envelopes
        int[][] E = cd.getEnvelopeScalefactors();
        int Le = cd.getNumEnv();
        int[] r = cd.getFreqRes();
        float a = (cd.getAmpRes() == 0) ? 2 : 1;
        int[] n = tables.getN();

        int len = Math.max(n[0], n[1]);
        float[][] Eorig = new float[len][Le];
        for (int l = 0; l < Le; l++) {
            for (int k = 0; k < n[r[l]]; k++) {
                Eorig[k][l] = 64.0f * (float) Math.pow(2, (E[l][k] / a));
            }
        }

        //noise
        int[][] Q = cd.getNoiseFloorData();
        int Lq = cd.getNumNoise();
        int Nq = tables.getNq();

        float[][] Qorig = new float[Nq][Lq];
        for (int l = 0; l < Lq; l++) {
            for (int k = 0; k < Nq; k++) {
                Qorig[k][l] = (float) Math.pow(2, NOISE_FLOOR_OFFSET - Q[l][k]);
            }
        }

        cd.setDequantData(Eorig, Qorig);
    }

    private void dequantCoupledChannels() {
        //envelopes
        //!!! indizes change: E0[l][k] -> Eorig[k][l]
        int[][] E0 = channel1.getEnvelopeScalefactors();
        int[][] E1 = channel2.getEnvelopeScalefactors();
        int ampRes = channel1.getAmpRes();
        float a = (ampRes == 0) ? 2 : 1;
        int Le = channel1.getNumEnv();
        int[] r = channel1.getFreqRes();
        int[] n = tables.getN();

        int len = Math.max(n[0], n[1]);
        float[][] Eleftorig = new float[len][Le];
        float[][] Erightorig = new float[len][Le];

        float f1, f2;
        for (int l = 0; l < Le; l++) {
            for (int k = 0; k < n[r[l]]; k++) {
                f1 = (float) E0[l][k] / a;
                f1 = (float) Math.pow(2, f1 + 1);
                f2 = (float) (PAN_OFFSET[ampRes] - E1[l][k]) / a;
                f2 = 1.0f + (float) Math.pow(2, f2);
                Eleftorig[k][l] = 64 * (f1 / f2);

                f2 = (float) (E1[l][k] - PAN_OFFSET[ampRes]) / a;
                f2 = 1.0f + (float) Math.pow(2, f2);
                Erightorig[k][l] = 64 * (f1 / f2);
            }
        }

        //noise
        //!!! indizes change: Q0[l][k] -> Qorig[k][l]
        int[][] Q0 = channel1.getNoiseFloorData();
        int[][] Q1 = channel2.getNoiseFloorData();
        int Lq = channel1.getNumNoise();
        int Nq = tables.getNq();

        float[][] Qleftorig = new float[Nq][Lq];
        float[][] Qrightorig = new float[Nq][Lq];
        for (int l = 0; l < Lq; l++) {
            for (int k = 0; k < Nq; k++) {
                f1 = (float) Math.pow(2, NOISE_FLOOR_OFFSET - Q0[l][k] + 1);
                f2 = 1.0f + (float) Math.pow(2, PAN_OFFSET[1] - Q1[l][k]);
                Qleftorig[k][l] = f1 / f2;

                f2 = 1.0f + (float) Math.pow(2, Q1[l][k] - PAN_OFFSET[1]);
                Qrightorig[k][l] = f1 / f2;
            }
        }

        channel1.setDequantData(Eleftorig, Qleftorig);
        channel2.setDequantData(Erightorig, Qrightorig);
    }

    public boolean isPSUsed() {
        return false;
    }

    //left/right: 1024 time samples
    //4.6.18.5
    public void process(float[] left, float[] right) {
        processChannel(0, left, channel1);
        if (right != null) processChannel(1, right, channel2);

        channel1.savePreviousData();
        if (channel2 != null) channel2.savePreviousData();
    }

    private void processChannel(int channel, float[] samples, ChannelData cd) {
        int lf = TIME_SLOTS[0] * RATE;
        int kx = tables.getKx();
        int kxPrev = tables.getKxPrev();
        int M = tables.getM();
        int Mprev = tables.getMPrev();
        int lTemp = cd.getLTemp();

        //copy old W
        for (int l = 0; l < T_HF_GEN; l++) {
            for (int k = 0; k < kxPrev; k++) {
                Xlow[k][l][0] = W[channel][k][l + lf - T_HF_GEN][0];
                Xlow[k][l][1] = W[channel][k][l + lf - T_HF_GEN][1];
            }
            for (int k = kxPrev; k < 32; k++) {
                Xlow[k][l][0] = 0;
                Xlow[k][l][1] = 0;
            }
        }
        //1. analysis QMF
        analysisFilter.process(channel, samples, W[channel]);

        //2. calculate Xlow
        for (int l = T_HF_GEN; l < lf + T_HF_GEN; l++) {
            for (int k = 0; k < kx; k++) {
                Xlow[k][l][0] = W[channel][k][l + lf - T_HF_GEN][0];
                Xlow[k][l][1] = W[channel][k][l + lf - T_HF_GEN][1];
            }
            for (int k = kx; k < 32; k++) {
                Xlow[k][l][0] = 0;
                Xlow[k][l][1] = 0;
            }
        }

        //3. HF generation
        HFGenerator.process(tables, cd, Xlow, Xhigh);

        //copy old Y
        for (int l = 0; l < lTemp; l++) {
            for (int k = 0; k < kxPrev; k++) {
                X[k][l][0] = Xlow[k][l + T_HF_ADJ][0];
                X[k][l][1] = Xlow[k][l + T_HF_ADJ][1];
            }
            for (int k = kxPrev; k < kxPrev + Mprev; k++) {
                X[k][l][0] = Y[channel][k][l + T_HF_ADJ + lf][0];
                X[k][l][1] = Y[channel][k][l + T_HF_ADJ + lf][1];
            }
            for (int k = kxPrev + Mprev; k < 64; k++) {
                X[k][l][0] = 0;
                X[k][l][1] = 0;
            }
        }

        //4. HF adjustment
        adjuster.process(header, tables, cd, Xhigh, Y[channel]);

        //5. calculate Y
        for (int l = lTemp; l < lf; l++) {
            for (int k = 0; k < kx; k++) {
                X[k][l][0] = Xlow[k][l + T_HF_ADJ][0];
                X[k][l][1] = Xlow[k][l + T_HF_ADJ][1];
            }
            for (int k = kx; k < kx + M; k++) {
                X[k][l][0] = Y[channel][k][l + T_HF_ADJ][0];
                X[k][l][1] = Y[channel][k][l + T_HF_ADJ][1];
            }
            for (int k = kx + M; k < 64; k++) {
                X[k][l][0] = 0;
                X[k][l][1] = 0;
            }
        }

        //6. synthesis QMF
        synthesisFilter.process(channel, X, samples);
    }
}

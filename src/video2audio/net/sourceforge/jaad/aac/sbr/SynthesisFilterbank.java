package net.sourceforge.jaad.aac.sbr;

class SynthesisFilterbank implements FilterbankTable, Constants {

    private final float[][][] COEFS;
    private final float[][] v; //for both channels
    private final float[] g, w;

    SynthesisFilterbank() {
        v = new float[2][1280];
        g = new float[640];
        w = new float[640];

        COEFS = new float[128][64][2];
        final double fac = 1.0 / 64.0;
        double tmp;
        for (int n = 0; n < 128; n++) {
            for (int k = 0; k < 64; k++) {
                tmp = Math.PI / 128.0 * (k + 0.5) * (2.0 * n - 255.0);
                COEFS[n][k][0] = (float) (fac * Math.cos(tmp));
                COEFS[n][k][1] = (float) (fac * Math.sin(tmp));
            }
        }
    }

    //in: 64 x 32 complex, out: 2048 time samples
    void process(int channel, float[][][] in, float[] out) {
        final float[] vc = v[channel];
        float tmp;
        int outOff = 0;
        for (int l = 0; l < TIME_SLOTS[0] * RATE; l++) {
            //shift samples by 128, oldest are discarded
            for (int n = 1279; n >= 128; n--) {
                vc[n] = vc[n - 128];
            }

            //multiply by matrix
            for (int n = 0; n < 128; n++) {
                vc[n] = (in[0][l][0] * COEFS[n][0][0]) - (in[0][l][1] * COEFS[n][0][1]);
                for (int k = 1; k < 64; k++) {
                    vc[n] += (in[k][l][0] * COEFS[n][k][0]) - (in[k][l][1] * COEFS[n][k][1]);
                }
            }

            //extract samples
            for (int n = 0; n < 5; n++) {
                for (int k = 0; k < 64; k++) {
                    g[128 * n + k] = vc[256 * n + k];
                    g[128 * n + 64 + k] = vc[256 * n + 192 + k];
                }
            }

            //multiply by window
            for (int n = 0; n < 640; n++) {
                w[n] = g[n] * WINDOW[n];
            }

            //sum samples
            for (int k = 0; k < 64; k++) {
                tmp = w[k];
                for (int n = 1; n < 10; n++) {
                    tmp += w[64 * n + k];
                }
                out[outOff++] = tmp;
            }
        }
    }
}

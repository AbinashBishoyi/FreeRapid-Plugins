package net.sourceforge.jaad.aac.sbr;

class AnalysisFilterbank implements FilterbankTable, Constants {

    private final float[][][] COEFS;
    private final float[][] x; //for both channels
    private final float[] z, u;

    AnalysisFilterbank() {
        x = new float[2][320];
        z = new float[320];
        u = new float[64];

        COEFS = new float[32][64][2];
        double tmp;
        for (int k = 0; k < 32; k++) {
            for (int n = 0; n < 64; n++) {
                tmp = Math.PI / 64.0 * (k + 0.5) * (2.0 * n - 0.5);
                COEFS[k][n][0] = (float) (2 * Math.cos(tmp));
                COEFS[k][n][1] = (float) (2 * Math.sin(tmp));
            }
        }
    }

    //in: 1024 time samples, out: 32 x 32 complex
    void process(int channel, float[] in, float[][][] out) {
        final float[] xc = x[channel];
        int inOff = 0;
        for (int l = 0; l < TIME_SLOTS[0] * RATE; l++) {
            //shift x by 32, oldest are discarded
            for (int n = 319; n >= 32; n--) {
                xc[n] = xc[n - 32];
            }

            //insert new samples at 0..31
            for (int n = 31; n >= 0; n--) {
                xc[n] = in[inOff++];
            }

            //multiply with window
            for (int n = 0; n < 320; n++) {
                z[n] = xc[n] * WINDOW[2 * n];
            }

            //sum samples, create u
            for (int n = 0; n < 64; n++) {
                u[n] = z[n];
                for (int j = 1; j <= 4; j++) {
                    u[n] = u[n] + z[n + j * 64];
                }
            }

            //calculate new subband samples
            for (int k = 0; k < 32; k++) {
                out[k][l][0] = u[0] * COEFS[k][0][0];
                out[k][l][1] = u[0] * COEFS[k][0][1];
                for (int n = 1; n < 64; n++) {
                    out[k][l][0] = out[k][l][0] + u[n] * COEFS[k][n][0];
                    out[k][l][1] = out[k][l][1] + u[n] * COEFS[k][n][1];
                }
            }
        }
    }
}

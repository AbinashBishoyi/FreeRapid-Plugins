package cz.vity.freerapid.plugins.services.cryptit.cypher;
/**
 * @author RickCL
 */
public class Copy {
    private static final int Nb = 4;

    // copy: copy state to out
    public static void copy(byte[] out, byte[][] state) {
        int outLoc = 0;
        for (int c = 0; c < Nb; c++) {
            for (int r = 0; r < 4; r++) {
                out[outLoc++] = state[r][c];
            }
        }
    }

    // copy: copy in to state
    public static void copy(byte[][] state, byte[] in) {
        int inLoc = 0;
        for (int c = 0; c < Nb; c++) {
            for (int r = 0; r < 4; r++) {
                state[r][c] = in[inLoc++];
            }
        }
    }
}
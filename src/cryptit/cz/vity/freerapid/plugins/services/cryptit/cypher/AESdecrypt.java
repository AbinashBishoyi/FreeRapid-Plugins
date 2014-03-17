package cz.vity.freerapid.plugins.services.cryptit.cypher;

import org.apache.commons.codec.binary.Base64;

/**
 * @author RickCL
 */
public class AESdecrypt {
    public final int Nb = 4; // words in a block, always 4 for now
    public int nk; // key length in words
    public int nr; // number of rounds, = Nk + 6
    AEStables tab; // all the tables needed for AES
    byte[] w; // the expanded key
    private int wCount; // position in w (= 4*Nb*(Nr+1) each encrypt)

    // AESdecrypt: constructor for class. Mainly expands key
    public AESdecrypt(byte[] key, int NkIn) {
        nk = NkIn; // words in a key, = 4, or 6, or 8
        nr = nk + 6; // corresponding number of rounds
        tab = new AEStables(); // class to give values of various functions
        w = new byte[4 * Nb * (nr + 1)]; // room for expanded key
        KeyExpansion(key, w); // length of w depends on Nr
    }

    // InvAddRoundKey: same as AddRoundKey, but backwards
    private void InvAddRoundKey(byte[][] state) {
        for (int c = Nb - 1; c >= 0; c--) {
            for (int r = 3; r >= 0; r--) {
                state[r][c] = (byte) (state[r][c] ^ w[--wCount]);
            }
        }
    }

    // InvCipher: actual AES decryption
    public void InvCipher(byte[] in, byte[] out) {
        wCount = 4 * Nb * (nr + 1); // count bytes during decryption
        byte[][] state = new byte[4][Nb]; // the state array
        Copy.copy(state, in); // actual component-wise copy
        InvAddRoundKey(state); // xor with expanded key
        for (int round = nr - 1; round >= 1; round--) {
            // Print.printArray("Start round " + (Nr - round) + ":", state);
            InvShiftRows(state); // mix up rows
            InvSubBytes(state); // inverse S-box substitution
            InvAddRoundKey(state); // xor with expanded key
            InvMixColumns(state); // complicated mix of columns
        }
        // Print.printArray("Start round " + Nr + ":", state);
        InvShiftRows(state); // mix up rows
        InvSubBytes(state); // inverse S-box substitution
        InvAddRoundKey(state); // xor with expanded key
        Copy.copy(out, state);
    }

    // InvMixColumns: complex and sophisticated mixing of columns
    private void InvMixColumns(byte[][] s) {
        int[] sp = new int[4];
        byte b0b = (byte) 0x0b;
        byte b0d = (byte) 0x0d;
        byte b09 = (byte) 0x09;
        byte b0e = (byte) 0x0e;
        for (int c = 0; c < 4; c++) {
            sp[0] = tab.FFMul(b0e, s[0][c]) ^ tab.FFMul(b0b, s[1][c]) ^ tab.FFMul(b0d, s[2][c]) ^ tab.FFMul(b09, s[3][c]);
            sp[1] = tab.FFMul(b09, s[0][c]) ^ tab.FFMul(b0e, s[1][c]) ^ tab.FFMul(b0b, s[2][c]) ^ tab.FFMul(b0d, s[3][c]);
            sp[2] = tab.FFMul(b0d, s[0][c]) ^ tab.FFMul(b09, s[1][c]) ^ tab.FFMul(b0e, s[2][c]) ^ tab.FFMul(b0b, s[3][c]);
            sp[3] = tab.FFMul(b0b, s[0][c]) ^ tab.FFMul(b0d, s[1][c]) ^ tab.FFMul(b09, s[2][c]) ^ tab.FFMul(b0e, s[3][c]);
            for (int i = 0; i < 4; i++) {
                s[i][c] = (byte) sp[i];
            }
        }
    }

    // InvShiftRows: right circular shift of rows 1, 2, 3 by 1, 2, 3
    private void InvShiftRows(byte[][] state) {
        byte[] t = new byte[4];
        for (int r = 1; r < 4; r++) {
            for (int c = 0; c < Nb; c++) {
                t[(c + r) % Nb] = state[r][c];
            }
            for (int c = 0; c < Nb; c++) {
                state[r][c] = t[c];
            }
        }
    }

    // InvSubBytes: apply inverse Sbox substitution to each byte of state
    private void InvSubBytes(byte[][] state) {
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < Nb; col++) {
                state[row][col] = tab.invSBox(state[row][col]);
            }
        }
    }

    // KeyExpansion: expand key, byte-oriented code, but tracks words
    // (the same as for encryption)
    private void KeyExpansion(byte[] key, byte[] w) {
        byte[] temp = new byte[4];
        // first just copy key to w
        int j = 0;
        while (j < 4 * nk) {
            w[j] = key[j++];
        }
        // here j == 4*Nk;
        int i;
        while (j < 4 * Nb * (nr + 1)) {
            i = j / 4; // j is always multiple of 4 here
            // handle everything word-at-a time, 4 bytes at a time
            for (int iTemp = 0; iTemp < 4; iTemp++) {
                temp[iTemp] = w[j - 4 + iTemp];
            }
            if (i % nk == 0) {
                byte ttemp, tRcon;
                byte oldtemp0 = temp[0];
                for (int iTemp = 0; iTemp < 4; iTemp++) {
                    if (iTemp == 3) {
                        ttemp = oldtemp0;
                    } else {
                        ttemp = temp[iTemp + 1];
                    }
                    if (iTemp == 0) {
                        tRcon = tab.Rcon(i / nk);
                    } else {
                        tRcon = 0;
                    }
                    temp[iTemp] = (byte) (tab.SBox(ttemp) ^ tRcon);
                }
            } else if (nk > 6 && i % nk == 4) {
                for (int iTemp = 0; iTemp < 4; iTemp++) {
                    temp[iTemp] = tab.SBox(temp[iTemp]);
                }
            }
            for (int iTemp = 0; iTemp < 4; iTemp++) {
                w[j + iTemp] = (byte) (w[j - 4 * nk + iTemp] ^ temp[iTemp]);
            }
            j = j + 4;
        }
    }

    public static String decrypt(String b64key, String ciphertext) {
        // alt: byte[] key = new byte[] { (byte) 55, (byte) 55, (byte) 107,
        // (byte) 47, (byte) 108, (byte) 65, (byte) 87, (byte) 72, (byte) 83,
        // (byte) 110, (byte) 116, (byte) 82, (byte) 89, (byte) 100, (byte) 111,
        // (byte) 110, (byte) 116, (byte) 115, (byte) 116, (byte) 101, (byte)
        // 97, (byte) 108, (byte) 112, (byte) (byte) 114 };
        byte[] key = Base64.decodeBase64(b64key.getBytes());
        byte[] cipher = new byte[ciphertext.length() / 2 + ciphertext.length() % 2];

        for (int i = 0; i < ciphertext.length(); i += 2) {
            String sub = ciphertext.substring(i, Math.min(ciphertext.length(), i + 2));
            cipher[i / 2] = (byte) Integer.parseInt(sub, 16);

        }

        AESdecrypt aes = new AESdecrypt(key, 6);
        int blockSize = 16;
        byte[] input = new byte[blockSize];
        byte[] output = new byte[blockSize];
        int blocks = 0;
        int rest = 0;
        while (true) {
            rest = cipher.length - blocks * blockSize;
            int cb = Math.min(rest, blockSize);
            input = new byte[blockSize];
            System.arraycopy(cipher, blocks * blockSize, input, 0, cb);
            aes.InvCipher(input, output);
            System.arraycopy(output, 0, cipher, blocks * blockSize, cb);
            if (rest <= blockSize) break;
            blocks++;

        }
        return new String(cipher).trim();

    }

}

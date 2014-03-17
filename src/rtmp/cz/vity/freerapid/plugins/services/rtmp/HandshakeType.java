package cz.vity.freerapid.plugins.services.rtmp;

import org.apache.mina.core.buffer.IoBuffer;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author ntoskrnl
 */
enum HandshakeType {

    TYPE_3(false) {
        @Override
        public void specialEncryption(final byte[] digest, final byte[] signature) {
        }
    },

    TYPE_6(true) {
        @Override
        public void specialEncryption(final byte[] digest, final byte[] signature) {
        }
    },

    TYPE_8(true) {
        @Override
        protected void specialEncryptionSub(final int[] v, final int keyid) {
            /* RTMPE type 8 uses XTEA on the regular signature
             * http://en.wikipedia.org/wiki/XTEA
             */
            final int DELTA = 0x9E3779B9;
            final int NUM_ROUNDS = 32;

            final int[] k = RTMPE8_KEYS[keyid];
            int v0 = v[0];
            int v1 = v[1];

            int sum = 0;
            for (int i = 0; i < NUM_ROUNDS; i++) {
                v0 += (((v1 << 4) ^ (v1 >>> 5)) + v1) ^ (sum + k[sum & 3]);
                sum += DELTA;
                v1 += (((v0 << 4) ^ (v0 >>> 5)) + v0) ^ (sum + k[(sum >>> 11) & 3]);
            }

            v[0] = v0;
            v[1] = v1;
        }
    },

    TYPE_9(true) {
        @Override
        protected void specialEncryptionSub(final int[] v, final int keyid) {
            /* RTMPE type 9 uses Blowfish on the regular signature
             * http://en.wikipedia.org/wiki/Blowfish_(cipher)
             */
            try {
                final Cipher c = Cipher.getInstance("Blowfish/ECB/NoPadding");
                c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(RTMPE9_KEYS[keyid], "Blowfish"));
                byte[] b = new byte[8];
                writeInt(b, v[0], 0);
                writeInt(b, v[1], 4);
                b = c.doFinal(b);
                v[0] = readInt(b, 0);
                v[1] = readInt(b, 4);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }

        private int readInt(final byte[] in, int index) {
            return ((in[index] & 0xFF) << 24) | ((in[++index] & 0xFF) << 16) | ((in[++index] & 0xFF) << 8) | (in[++index] & 0xFF);
        }

        private void writeInt(final byte[] out, final int value, int index) {
            out[index] = (byte) (0xFF & (value >> 24));
            out[++index] = (byte) (0xFF & (value >> 16));
            out[++index] = (byte) (0xFF & (value >> 8));
            out[++index] = (byte) (0xFF & (value));
        }
    };

    private final boolean encrypted;

    private HandshakeType(final boolean encrypted) {
        this.encrypted = encrypted;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void specialEncryption(final byte[] digest, final byte[] signature0) {
        final IoBuffer signature = IoBuffer.wrap(signature0);
        for (int i = 0; i < Handshake.SHA256_LEN; i += 8) {
            final int[] v = {
                    Utils.readInt32Reverse(signature),
                    Utils.readInt32Reverse(signature)
            };
            final int keyid = (digest[i] & 0xff) % 15;

            specialEncryptionSub(v, keyid);

            signature.position(signature.position() - 8);
            Utils.writeInt32Reverse(signature, v[0]);
            Utils.writeInt32Reverse(signature, v[1]);
        }
    }

    protected void specialEncryptionSub(final int[] v, final int keyid) {
    }

    public static HandshakeType valueOf(final int number) {
        switch (number) {
            case 3:
                return TYPE_3;
            case 6:
                return TYPE_6;
            case 8:
                return TYPE_8;
            case 9:
                return TYPE_9;
            default:
                throw new RuntimeException("Unknown RTMP handshake type: " + number);
        }
    }

    private final static int[][] RTMPE8_KEYS = {
            {0xbff034b2, 0x11d9081f, 0xccdfb795, 0x748de732},
            {0x086a5eb6, 0x1743090e, 0x6ef05ab8, 0xfe5a39e2},
            {0x7b10956f, 0x76ce0521, 0x2388a73a, 0x440149a1},
            {0xa943f317, 0xebf11bb2, 0xa691a5ee, 0x17f36339},
            {0x7a30e00a, 0xb529e22c, 0xa087aea5, 0xc0cb79ac},
            {0xbdce0c23, 0x2febdeff, 0x1cfaae16, 0x1123239d},
            {0x55dd3f7b, 0x77e7e62e, 0x9bb8c499, 0xc9481ee4},
            {0x407bb6b4, 0x71e89136, 0xa7aebf55, 0xca33b839},
            {0xfcf6bdc3, 0xb63c3697, 0x7ce4f825, 0x04d959b2},
            {0x28e091fd, 0x41954c4c, 0x7fb7db00, 0xe3a066f8},
            {0x57845b76, 0x4f251b03, 0x46d45bcd, 0xa2c30d29},
            {0x0acceef8, 0xda55b546, 0x03473452, 0x5863713b},
            {0xb82075dc, 0xa75f1fee, 0xd84268e8, 0xa72a44cc},
            {0x07cf6e9e, 0xa16d7b25, 0x9fa7ae6c, 0xd92f5629},
            {0xfeb1eae4, 0x8c8c3ce1, 0x4e0064a7, 0x6a387c2a},
            {0x893a9427, 0xcc3013a2, 0xf106385b, 0xa829f927},
    };

    private final static byte[][] RTMPE9_KEYS = {
            {(byte) 0x79, (byte) 0x34, (byte) 0x77, (byte) 0x4c, (byte) 0x67, (byte) 0xd1,
                    (byte) 0x38, (byte) 0x3a, (byte) 0xdf, (byte) 0xb3, (byte) 0x56, (byte) 0xbe,
                    (byte) 0x8b, (byte) 0x7b, (byte) 0xd0, (byte) 0x24, (byte) 0x38, (byte) 0xe0,
                    (byte) 0x73, (byte) 0x58, (byte) 0x41, (byte) 0x5d, (byte) 0x69, (byte) 0x67,},
            {(byte) 0x46, (byte) 0xf6, (byte) 0xb4, (byte) 0xcc, (byte) 0x01, (byte) 0x93,
                    (byte) 0xe3, (byte) 0xa1, (byte) 0x9e, (byte) 0x7d, (byte) 0x3c, (byte) 0x65,
                    (byte) 0x55, (byte) 0x86, (byte) 0xfd, (byte) 0x09, (byte) 0x8f, (byte) 0xf7,
                    (byte) 0xb3, (byte) 0xc4, (byte) 0x6f, (byte) 0x41, (byte) 0xca, (byte) 0x5c,},
            {(byte) 0x1a, (byte) 0xe7, (byte) 0xe2, (byte) 0xf3, (byte) 0xf9, (byte) 0x14,
                    (byte) 0x79, (byte) 0x94, (byte) 0xc0, (byte) 0xd3, (byte) 0x97, (byte) 0x43,
                    (byte) 0x08, (byte) 0x7b, (byte) 0xb3, (byte) 0x84, (byte) 0x43, (byte) 0x2f,
                    (byte) 0x9d, (byte) 0x84, (byte) 0x3f, (byte) 0x21, (byte) 0x01, (byte) 0x9b,},
            {(byte) 0xd3, (byte) 0xe3, (byte) 0x54, (byte) 0xb0, (byte) 0xf7, (byte) 0x1d,
                    (byte) 0xf6, (byte) 0x2b, (byte) 0x5a, (byte) 0x43, (byte) 0x4d, (byte) 0x04,
                    (byte) 0x83, (byte) 0x64, (byte) 0x3e, (byte) 0x0d, (byte) 0x59, (byte) 0x2f,
                    (byte) 0x61, (byte) 0xcb, (byte) 0xb1, (byte) 0x6a, (byte) 0x59, (byte) 0x0d,},
            {(byte) 0xc8, (byte) 0xc1, (byte) 0xe9, (byte) 0xb8, (byte) 0x16, (byte) 0x56,
                    (byte) 0x99, (byte) 0x21, (byte) 0x7b, (byte) 0x5b, (byte) 0x36, (byte) 0xb7,
                    (byte) 0xb5, (byte) 0x9b, (byte) 0xdf, (byte) 0x06, (byte) 0x49, (byte) 0x2c,
                    (byte) 0x97, (byte) 0xf5, (byte) 0x95, (byte) 0x48, (byte) 0x85, (byte) 0x7e,},
            {(byte) 0xeb, (byte) 0xe5, (byte) 0xe6, (byte) 0x2e, (byte) 0xa4, (byte) 0xba,
                    (byte) 0xd4, (byte) 0x2c, (byte) 0xf2, (byte) 0x16, (byte) 0xe0, (byte) 0x8f,
                    (byte) 0x66, (byte) 0x23, (byte) 0xa9, (byte) 0x43, (byte) 0x41, (byte) 0xce,
                    (byte) 0x38, (byte) 0x14, (byte) 0x84, (byte) 0x95, (byte) 0x00, (byte) 0x53,},
            {(byte) 0x66, (byte) 0xdb, (byte) 0x90, (byte) 0xf0, (byte) 0x3b, (byte) 0x4f,
                    (byte) 0xf5, (byte) 0x6f, (byte) 0xe4, (byte) 0x9c, (byte) 0x20, (byte) 0x89,
                    (byte) 0x35, (byte) 0x5e, (byte) 0xd2, (byte) 0xb2, (byte) 0xc3, (byte) 0x9e,
                    (byte) 0x9f, (byte) 0x7f, (byte) 0x63, (byte) 0xb2, (byte) 0x28, (byte) 0x81,},
            {(byte) 0xbb, (byte) 0x20, (byte) 0xac, (byte) 0xed, (byte) 0x2a, (byte) 0x04,
                    (byte) 0x6a, (byte) 0x19, (byte) 0x94, (byte) 0x98, (byte) 0x9b, (byte) 0xc8,
                    (byte) 0xff, (byte) 0xcd, (byte) 0x93, (byte) 0xef, (byte) 0xc6, (byte) 0x0d,
                    (byte) 0x56, (byte) 0xa7, (byte) 0xeb, (byte) 0x13, (byte) 0xd9, (byte) 0x30,},
            {(byte) 0xbc, (byte) 0xf2, (byte) 0x43, (byte) 0x82, (byte) 0x09, (byte) 0x40,
                    (byte) 0x8a, (byte) 0x87, (byte) 0x25, (byte) 0x43, (byte) 0x6d, (byte) 0xe6,
                    (byte) 0xbb, (byte) 0xa4, (byte) 0xb9, (byte) 0x44, (byte) 0x58, (byte) 0x3f,
                    (byte) 0x21, (byte) 0x7c, (byte) 0x99, (byte) 0xbb, (byte) 0x3f, (byte) 0x24,},
            {(byte) 0xec, (byte) 0x1a, (byte) 0xaa, (byte) 0xcd, (byte) 0xce, (byte) 0xbd,
                    (byte) 0x53, (byte) 0x11, (byte) 0xd2, (byte) 0xfb, (byte) 0x83, (byte) 0xb6,
                    (byte) 0xc3, (byte) 0xba, (byte) 0xab, (byte) 0x4f, (byte) 0x62, (byte) 0x79,
                    (byte) 0xe8, (byte) 0x65, (byte) 0xa9, (byte) 0x92, (byte) 0x28, (byte) 0x76,},
            {(byte) 0xc6, (byte) 0x0c, (byte) 0x30, (byte) 0x03, (byte) 0x91, (byte) 0x18,
                    (byte) 0x2d, (byte) 0x7b, (byte) 0x79, (byte) 0xda, (byte) 0xe1, (byte) 0xd5,
                    (byte) 0x64, (byte) 0x77, (byte) 0x9a, (byte) 0x12, (byte) 0xc5, (byte) 0xb1,
                    (byte) 0xd7, (byte) 0x91, (byte) 0x4f, (byte) 0x96, (byte) 0x4c, (byte) 0xa3,},
            {(byte) 0xd7, (byte) 0x7c, (byte) 0x2a, (byte) 0xbf, (byte) 0xa6, (byte) 0xe7,
                    (byte) 0x85, (byte) 0x7c, (byte) 0x45, (byte) 0xad, (byte) 0xff, (byte) 0x12,
                    (byte) 0x94, (byte) 0xd8, (byte) 0xde, (byte) 0xa4, (byte) 0x5c, (byte) 0x3d,
                    (byte) 0x79, (byte) 0xa4, (byte) 0x44, (byte) 0x02, (byte) 0x5d, (byte) 0x22,},
            {(byte) 0x16, (byte) 0x19, (byte) 0x0d, (byte) 0x81, (byte) 0x6a, (byte) 0x4c,
                    (byte) 0xc7, (byte) 0xf8, (byte) 0xb8, (byte) 0xf9, (byte) 0x4e, (byte) 0xcd,
                    (byte) 0x2c, (byte) 0x9e, (byte) 0x90, (byte) 0x84, (byte) 0xb2, (byte) 0x08,
                    (byte) 0x25, (byte) 0x60, (byte) 0xe1, (byte) 0x1e, (byte) 0xae, (byte) 0x18,},
            {(byte) 0xe9, (byte) 0x7c, (byte) 0x58, (byte) 0x26, (byte) 0x1b, (byte) 0x51,
                    (byte) 0x9e, (byte) 0x49, (byte) 0x82, (byte) 0x60, (byte) 0x61, (byte) 0xfc,
                    (byte) 0xa0, (byte) 0xa0, (byte) 0x1b, (byte) 0xcd, (byte) 0xf5, (byte) 0x05,
                    (byte) 0xd6, (byte) 0xa6, (byte) 0x6d, (byte) 0x07, (byte) 0x88, (byte) 0xa3,},
            {(byte) 0x2b, (byte) 0x97, (byte) 0x11, (byte) 0x8b, (byte) 0xd9, (byte) 0x4e,
                    (byte) 0xd9, (byte) 0xdf, (byte) 0x20, (byte) 0xe3, (byte) 0x9c, (byte) 0x10,
                    (byte) 0xe6, (byte) 0xa1, (byte) 0x35, (byte) 0x21, (byte) 0x11, (byte) 0xf9,
                    (byte) 0x13, (byte) 0x0d, (byte) 0x0b, (byte) 0x24, (byte) 0x65, (byte) 0xb2,},
            {(byte) 0x53, (byte) 0x6a, (byte) 0x4c, (byte) 0x54, (byte) 0xac, (byte) 0x8b,
                    (byte) 0x9b, (byte) 0xb8, (byte) 0x97, (byte) 0x29, (byte) 0xfc, (byte) 0x60,
                    (byte) 0x2c, (byte) 0x5b, (byte) 0x3a, (byte) 0x85, (byte) 0x68, (byte) 0xb5,
                    (byte) 0xaa, (byte) 0x6a, (byte) 0x44, (byte) 0xcd, (byte) 0x3f, (byte) 0xa7,},
    };

}

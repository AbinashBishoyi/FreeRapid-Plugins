package cz.vity.freerapid.plugins.services.rtmp;

/**
 * Support class for secureToken.
 *
 * @author Ma Bingyao <andot@ujn.edu.cn>
 * @author JPEXS
 * @author ntoskrnl
 * @see RtmpSession#secureToken
 */
class XXTEA {

    public static String decrypt(String data, String key) {
        return new String(decrypt(Utils.fromHex(data), key.getBytes()));
    }

    public static byte[] decrypt(byte[] data, byte[] key) {
        return toByteArray(decrypt(toIntArray(data), toIntArray(key)));
    }

    public static int[] decrypt(int[] v, int[] k) {
        int n = v.length - 1;
        if (n < 1) {
            return v;
        }
        if (k.length < 4) {
            int[] key = new int[4];
            System.arraycopy(k, 0, key, 0, k.length);
            k = key;
        }
        int z, y = v[0], delta = 0x9E3779B9, sum, e;
        int p, q = 6 + 52 / (n + 1);
        sum = q * delta;
        while (sum != 0) {
            e = sum >>> 2 & 3;
            for (p = n; p > 0; p--) {
                z = v[p - 1];
                y = v[p] -= (z >>> 5 ^ y << 2) + (y >>> 3 ^ z << 4) ^ (sum ^ y) + (k[p & 3 ^ e] ^ z);
            }
            z = v[n];
            y = v[0] -= (z >>> 5 ^ y << 2) + (y >>> 3 ^ z << 4) ^ (sum ^ y) + (k[p & 3 ^ e] ^ z);
            sum = sum - delta;
        }
        return v;
    }

    private static int[] toIntArray(byte[] data) {
        int n = (((data.length & 3) == 0) ? (data.length >>> 2) : ((data.length >>> 2) + 1));
        int[] result = new int[n];
        n = data.length;
        for (int i = 0; i < n; i++) {
            result[i >>> 2] |= (0x000000ff & data[i]) << ((i & 3) << 3);
        }
        return result;
    }

    private static byte[] toByteArray(int[] data) {
        int n = data.length << 2;
        byte[] result = new byte[n];
        for (int i = 0; i < n; i++) {
            result[i] = (byte) (data[i >>> 2] >>> ((i & 3) << 3));
        }
        return result;
    }

}

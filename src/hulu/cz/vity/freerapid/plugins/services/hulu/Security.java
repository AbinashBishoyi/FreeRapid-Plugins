package cz.vity.freerapid.plugins.services.hulu;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;

/**
 * - Decompiled from http://www.hulu.com/site-player/sec_as3.swf
 * - Translated from ActionScript to Java
 * - Due to the major differences in arrays between the two languages,
 * most of the arrays had to be changed to Lists or Maps
 *
 * @author ntoskrnl
 */
class Security {
    public static String decrypt(final String s) {
        return S.dec(s).trim();
    }
}

class Thing {
    public int rounds;
    public Map<Integer, Long> rd_key;
}

class RD {

    public static Map<Long, Long> Te0 = new HashMap<Long, Long>();
    public static Map<Long, Long> Te1 = new HashMap<Long, Long>();
    public static Map<Long, Long> Te2 = new HashMap<Long, Long>();
    public static Map<Long, Long> Te3 = new HashMap<Long, Long>();
    public static Map<Long, Long> Te4 = new HashMap<Long, Long>();
    public static Map<Long, Long> Td0 = new HashMap<Long, Long>();
    public static Map<Long, Long> Td1 = new HashMap<Long, Long>();
    public static Map<Long, Long> Td2 = new HashMap<Long, Long>();
    public static Map<Long, Long> Td3 = new HashMap<Long, Long>();
    public static Map<Long, Long> Td4 = new HashMap<Long, Long>();

    private static int[] alog = new int[0x0100 + 0xFF];
    private static int[] log = new int[0x0100];

    static {
        update();
    }

    public static void update() {
        int i;
        int x;
        int s;
        int[] S = new int[0x0100];
        int[] Si = new int[0x0100];
        i = 0;
        while (i < 0x0100) {
            alog[i] = 0;
            alog[(i + 0xFF)] = 0;
            log[i] = 0;
            S[i] = 0;
            Si[i] = 0;
            i = (i + 1);
        }
        int j = 1;
        i = 0;
        while (i < 0x0100) {
            alog[i] = j;
            alog[(i + 0xFF)] = j;
            log[j] = i;
            j = ((j ^ (j << 1)) ^ (((j & 128) != 0) ? 27 : 0)) & 255;
            i = (i + 1);
        }
        log[1] = 0;
        i = 0;
        while (i < 0x0100) {
            x = ((i) != 0) ? alog[(0xFF - log[i])] : 0;
            x = (x ^ ((((x << 1) ^ (x << 2)) ^ (x << 3)) ^ (x << 4)));
            x = (99 ^ (x ^ (x >> 8)));
            S[i] = (x & 0xFF);
            Si[(x & 0xFF)] = i;
            i = (i + 1);
        }
        int[][] G = {{2, 1, 1, 3}, {3, 2, 1, 1}, {1, 3, 2, 1}, {1, 1, 3, 2}, {1, 1, 1, 1}};
        int[][] Gi = {{14, 9, 13, 11}, {11, 14, 9, 13}, {13, 11, 14, 9}, {9, 13, 11, 14}, {1, 1, 1, 1}};
        int t = 0;
        while (t < 0x0100) {
            s = S[t];
            Te0.put((long) t, mul4(s, G[0]));
            Te1.put((long) t, mul4(s, G[1]));
            Te2.put((long) t, mul4(s, G[2]));
            Te3.put((long) t, mul4(s, G[3]));
            Te4.put((long) t, mul4(s, G[4]));
            s = Si[t];
            Td0.put((long) t, mul4(s, Gi[0]));
            Td1.put((long) t, mul4(s, Gi[1]));
            Td2.put((long) t, mul4(s, Gi[2]));
            Td3.put((long) t, mul4(s, Gi[3]));
            Td4.put((long) t, mul4(s, Gi[4]));
            t = (t + 1);
        }
    }

    private static long mul4(int _arg1, int[] _arg2) {
        String _local6;
        if (_arg1 == 0) {
            return (0);
        }
        _arg1 = log[(_arg1 & 0xFF)];
        int[] _local3 = {((_arg2[0]) != 0) ? (alog[((_arg1 + log[(_arg2[0] & 0xFF)]) % 0xFF)] & 0xFF) : 0, ((_arg2[1]) != 0) ? (alog[((_arg1 + log[(_arg2[1] & 0xFF)]) % 0xFF)] & 0xFF) : 0, ((_arg2[2]) != 0) ? (alog[((_arg1 + log[(_arg2[2] & 0xFF)]) % 0xFF)] & 0xFF) : 0, ((_arg2[3]) != 0) ? (alog[((_arg1 + log[(_arg2[3] & 0xFF)]) % 0xFF)] & 0xFF) : 0};
        String _local4 = "";
        int _local5 = 0;
        while (_local5 < 4) {
            _local6 = Integer.toHexString(_local3[_local5]);
            _local4 = _local4 + (_local6.length() == 1 ? ("0" + _local6) : _local6);
            _local5++;
        }
        return (int) Long.parseLong(_local4, 16);
    }

    public static String _YBAJiRcs() {
        return ("6fe8131ca9b0");
    }

}

class R {

    public static Map<Long, Long> Te0 = RD.Te0;
    public static Map<Long, Long> Te1 = RD.Te1;
    public static Map<Long, Long> Te2 = RD.Te2;
    public static Map<Long, Long> Te3 = RD.Te3;
    public static Map<Long, Long> Te4 = RD.Te4;
    public static Map<Long, Long> Td0 = RD.Td0;
    public static Map<Long, Long> Td1 = RD.Td1;
    public static Map<Long, Long> Td2 = RD.Td2;
    public static Map<Long, Long> Td3 = RD.Td3;
    public static Map<Long, Long> Td4 = RD.Td4;
    public static long[] rcon = {16777216, 33554432, 67108864, 134217728, 268435456, 536870912, 1073741824, 2147483648L, 452984832, 905969664};

    static {
        __0();
        R._0(null);
    }

    public static String ___() {
        return ("f5bc08c1c9ebaf65f039e1");
    }

    public static void __0() {
        LinkedList<String> _local2 = S.copyrighted_strings;
        _local2.addFirst(_local2.get(_local2.size() - 2).replace('e', 'f'));
    }

    public static int sek(String _arg1, int _arg2, Thing _arg3) {
        long _local4;
        int _local5 = 0;
        int _local6 = 0;
        if ((((_arg1 == null)) || ((_arg3 == null)))) {
            return (-1);
        }
        if (((((!((_arg2 == 128))) && (!((_arg2 == 192))))) && (!((_arg2 == 0x0100))))) {
            return (-2);
        }
        if (_arg2 == 128) {
            _arg3.rounds = 10;
        } else {
            if (_arg2 == 192) {
                _arg3.rounds = 12;
            } else {
                _arg3.rounds = 14;
            }
        }
        _arg3.rd_key.put(0, Long.parseLong(_arg1.substring(0, 8), 16));
        _arg3.rd_key.put(1, Long.parseLong(_arg1.substring(8, 16), 16));
        _arg3.rd_key.put(2, Long.parseLong(_arg1.substring(16, 24), 16));
        _arg3.rd_key.put(3, Long.parseLong(_arg1.substring(24, 32), 16));
        if (_arg2 == 128) {
            while (true) {
                _local4 = _arg3.rd_key.get(3 + _local6);
                _arg3.rd_key.put(4 + _local6, (((((_arg3.rd_key.get(_local6) ^ (Te4.get(((_local4 >> 16) & 0xFF)) & 4278190080L)) ^ (Te4.get(((_local4 >> 8) & 0xFF)) & 0xFF0000)) ^ (Te4.get((_local4 & 0xFF)) & 0xFF00)) ^ (Te4.get(((_local4 >> 24) & 0xFF)) & 0xFF)) ^ rcon[_local5]));
                _arg3.rd_key.put(5 + _local6, (_arg3.rd_key.get(1 + _local6) ^ _arg3.rd_key.get(4 + _local6)));
                _arg3.rd_key.put(6 + _local6, (_arg3.rd_key.get(2 + _local6) ^ _arg3.rd_key.get(5 + _local6)));
                _arg3.rd_key.put(7 + _local6, (_arg3.rd_key.get(3 + _local6) ^ _arg3.rd_key.get(6 + _local6)));
                ++_local5;
                if (_local5 == 10) {
                    return (0);
                }
                _local6 = (_local6 + 4);
            }
        }
        _arg3.rd_key.put(4, Long.parseLong(_arg1.substring(32, 40), 16));
        _arg3.rd_key.put(5, Long.parseLong(_arg1.substring(40, 48), 16));
        if (_arg2 == 192) {
            while (true) {
                _local4 = _arg3.rd_key.get(5 + _local6);
                _arg3.rd_key.put(6 + _local6, (((((_arg3.rd_key.get(_local6) ^ (Te4.get(((_local4 >> 16) & 0xFF)) & 4278190080L)) ^ (Te4.get(((_local4 >> 8) & 0xFF)) & 0xFF0000)) ^ (Te4.get((_local4 & 0xFF)) & 0xFF00)) ^ (Te4.get(((_local4 >> 24) & 0xFF)) & 0xFF)) ^ rcon[_local5]));
                _arg3.rd_key.put(7 + _local6, (_arg3.rd_key.get(1 + _local6) ^ _arg3.rd_key.get(6 + _local6)));
                _arg3.rd_key.put(8 + _local6, (_arg3.rd_key.get(2 + _local6) ^ _arg3.rd_key.get(7 + _local6)));
                _arg3.rd_key.put(9 + _local6, (_arg3.rd_key.get(3 + _local6) ^ _arg3.rd_key.get(8 + _local6)));
                ++_local5;
                if (_local5 == 8) {
                    return (0);
                }
                _arg3.rd_key.put(10 + _local6, (_arg3.rd_key.get(4 + _local6) ^ _arg3.rd_key.get(9 + _local6)));
                _arg3.rd_key.put(11 + _local6, (_arg3.rd_key.get(5 + _local6) ^ _arg3.rd_key.get(10 + _local6)));
                _local6 = (_local6 + 6);
            }
        }
        _arg3.rd_key.put(6, Long.parseLong(_arg1.substring(48, 56), 16));
        _arg3.rd_key.put(7, Long.parseLong(_arg1.substring(56, 64), 16));
        if (_arg2 == 0x0100) {
            while (true) {
                _local4 = _arg3.rd_key.get(7 + _local6);
                _arg3.rd_key.put(8 + _local6, (((((_arg3.rd_key.get(_local6) ^ (Te4.get(((_local4 >> 16) & 0xFF)) & 4278190080L)) ^ (Te4.get(((_local4 >> 8) & 0xFF)) & 0xFF0000)) ^ (Te4.get((_local4 & 0xFF)) & 0xFF00)) ^ (Te4.get(((_local4 >> 24) & 0xFF)) & 0xFF)) ^ rcon[_local5]));
                _arg3.rd_key.put(9 + _local6, (_arg3.rd_key.get(1 + _local6) ^ _arg3.rd_key.get(8 + _local6)));
                _arg3.rd_key.put(10 + _local6, (_arg3.rd_key.get(2 + _local6) ^ _arg3.rd_key.get(9 + _local6)));
                _arg3.rd_key.put(11 + _local6, (_arg3.rd_key.get(3 + _local6) ^ _arg3.rd_key.get(10 + _local6)));
                ++_local5;
                if (_local5 == 7) {
                    return (0);
                }
                _local4 = _arg3.rd_key.get(11 + _local6);
                _arg3.rd_key.put(12 + _local6, ((((_arg3.rd_key.get(4 + _local6) ^ (Te4.get(((_local4 >> 24) & 0xFF)) & 4278190080L)) ^ (Te4.get(((_local4 >> 16) & 0xFF)) & 0xFF0000)) ^ (Te4.get(((_local4 >> 8) & 0xFF)) & 0xFF00)) ^ (Te4.get((_local4 & 0xFF)) & 0xFF)));
                _arg3.rd_key.put(13 + _local6, (_arg3.rd_key.get(5 + _local6) ^ _arg3.rd_key.get(12 + _local6)));
                _arg3.rd_key.put(14 + _local6, (_arg3.rd_key.get(6 + _local6) ^ _arg3.rd_key.get(13 + _local6)));
                _arg3.rd_key.put(15 + _local6, (_arg3.rd_key.get(7 + _local6) ^ _arg3.rd_key.get(14 + _local6)));
                _local6 = (_local6 + 8);
            }
        }
        return (0);
    }

    public static String _() {
        return ("592d53a30def7fced26c");
    }

    public static String e(String _arg1, Thing _arg2) {
        long _local5;
        long _local6;
        long _local7;
        long _local8;
        long _local9;
        long _local10;
        long _local11;
        long _local12;
        int _local4;
        if ((((_arg1 == null)) || ((_arg2 == null)))) {
            return null;
        }
        _local5 = (Long.parseLong(_arg1.substring(0, 8), 16) ^ _arg2.rd_key.get(0));
        _local6 = (Long.parseLong(_arg1.substring(8, 16), 16) ^ _arg2.rd_key.get(1));
        _local7 = (Long.parseLong(_arg1.substring(16, 24), 16) ^ _arg2.rd_key.get(2));
        _local8 = (Long.parseLong(_arg1.substring(24, 32), 16) ^ _arg2.rd_key.get(3));
        _local9 = ((((Td0.get(((_local5 >> 24) & 0xFF)) ^ Td1.get(((_local8 >> 16) & 0xFF))) ^ Td2.get(((_local7 >> 8) & 0xFF))) ^ Td3.get((_local6 & 0xFF))) ^ _arg2.rd_key.get(4));
        _local10 = ((((Td0.get(((_local6 >> 24) & 0xFF)) ^ Td1.get(((_local5 >> 16) & 0xFF))) ^ Td2.get(((_local8 >> 8) & 0xFF))) ^ Td3.get((_local7 & 0xFF))) ^ _arg2.rd_key.get(5));
        _local11 = ((((Td0.get(((_local7 >> 24) & 0xFF)) ^ Td1.get(((_local6 >> 16) & 0xFF))) ^ Td2.get(((_local5 >> 8) & 0xFF))) ^ Td3.get((_local8 & 0xFF))) ^ _arg2.rd_key.get(6));
        _local12 = ((((Td0.get(((_local8 >> 24) & 0xFF)) ^ Td1.get(((_local7 >> 16) & 0xFF))) ^ Td2.get(((_local6 >> 8) & 0xFF))) ^ Td3.get((_local5 & 0xFF))) ^ _arg2.rd_key.get(7));
        _local5 = ((((Td0.get(((_local9 >> 24) & 0xFF)) ^ Td1.get(((_local12 >> 16) & 0xFF))) ^ Td2.get(((_local11 >> 8) & 0xFF))) ^ Td3.get((_local10 & 0xFF))) ^ _arg2.rd_key.get(8));
        _local6 = ((((Td0.get(((_local10 >> 24) & 0xFF)) ^ Td1.get(((_local9 >> 16) & 0xFF))) ^ Td2.get(((_local12 >> 8) & 0xFF))) ^ Td3.get((_local11 & 0xFF))) ^ _arg2.rd_key.get(9));
        _local7 = ((((Td0.get(((_local11 >> 24) & 0xFF)) ^ Td1.get(((_local10 >> 16) & 0xFF))) ^ Td2.get(((_local9 >> 8) & 0xFF))) ^ Td3.get((_local12 & 0xFF))) ^ _arg2.rd_key.get(10));
        _local8 = ((((Td0.get(((_local12 >> 24) & 0xFF)) ^ Td1.get(((_local11 >> 16) & 0xFF))) ^ Td2.get(((_local10 >> 8) & 0xFF))) ^ Td3.get((_local9 & 0xFF))) ^ _arg2.rd_key.get(11));
        _local9 = ((((Td0.get(((_local5 >> 24) & 0xFF)) ^ Td1.get(((_local8 >> 16) & 0xFF))) ^ Td2.get(((_local7 >> 8) & 0xFF))) ^ Td3.get((_local6 & 0xFF))) ^ _arg2.rd_key.get(12));
        _local10 = ((((Td0.get(((_local6 >> 24) & 0xFF)) ^ Td1.get(((_local5 >> 16) & 0xFF))) ^ Td2.get(((_local8 >> 8) & 0xFF))) ^ Td3.get((_local7 & 0xFF))) ^ _arg2.rd_key.get(13));
        _local11 = ((((Td0.get(((_local7 >> 24) & 0xFF)) ^ Td1.get(((_local6 >> 16) & 0xFF))) ^ Td2.get(((_local5 >> 8) & 0xFF))) ^ Td3.get((_local8 & 0xFF))) ^ _arg2.rd_key.get(14));
        _local12 = ((((Td0.get(((_local8 >> 24) & 0xFF)) ^ Td1.get(((_local7 >> 16) & 0xFF))) ^ Td2.get(((_local6 >> 8) & 0xFF))) ^ Td3.get((_local5 & 0xFF))) ^ _arg2.rd_key.get(15));
        _local5 = ((((Td0.get(((_local9 >> 24) & 0xFF)) ^ Td1.get(((_local12 >> 16) & 0xFF))) ^ Td2.get(((_local11 >> 8) & 0xFF))) ^ Td3.get((_local10 & 0xFF))) ^ _arg2.rd_key.get(16));
        _local6 = ((((Td0.get(((_local10 >> 24) & 0xFF)) ^ Td1.get(((_local9 >> 16) & 0xFF))) ^ Td2.get(((_local12 >> 8) & 0xFF))) ^ Td3.get((_local11 & 0xFF))) ^ _arg2.rd_key.get(17));
        _local7 = ((((Td0.get(((_local11 >> 24) & 0xFF)) ^ Td1.get(((_local10 >> 16) & 0xFF))) ^ Td2.get(((_local9 >> 8) & 0xFF))) ^ Td3.get((_local12 & 0xFF))) ^ _arg2.rd_key.get(18));
        _local8 = ((((Td0.get(((_local12 >> 24) & 0xFF)) ^ Td1.get(((_local11 >> 16) & 0xFF))) ^ Td2.get(((_local10 >> 8) & 0xFF))) ^ Td3.get((_local9 & 0xFF))) ^ _arg2.rd_key.get(19));
        _local9 = ((((Td0.get(((_local5 >> 24) & 0xFF)) ^ Td1.get(((_local8 >> 16) & 0xFF))) ^ Td2.get(((_local7 >> 8) & 0xFF))) ^ Td3.get((_local6 & 0xFF))) ^ _arg2.rd_key.get(20));
        _local10 = ((((Td0.get(((_local6 >> 24) & 0xFF)) ^ Td1.get(((_local5 >> 16) & 0xFF))) ^ Td2.get(((_local8 >> 8) & 0xFF))) ^ Td3.get((_local7 & 0xFF))) ^ _arg2.rd_key.get(21));
        _local11 = ((((Td0.get(((_local7 >> 24) & 0xFF)) ^ Td1.get(((_local6 >> 16) & 0xFF))) ^ Td2.get(((_local5 >> 8) & 0xFF))) ^ Td3.get((_local8 & 0xFF))) ^ _arg2.rd_key.get(22));
        _local12 = ((((Td0.get(((_local8 >> 24) & 0xFF)) ^ Td1.get(((_local7 >> 16) & 0xFF))) ^ Td2.get(((_local6 >> 8) & 0xFF))) ^ Td3.get((_local5 & 0xFF))) ^ _arg2.rd_key.get(23));
        _local5 = ((((Td0.get(((_local9 >> 24) & 0xFF)) ^ Td1.get(((_local12 >> 16) & 0xFF))) ^ Td2.get(((_local11 >> 8) & 0xFF))) ^ Td3.get((_local10 & 0xFF))) ^ _arg2.rd_key.get(24));
        _local6 = ((((Td0.get(((_local10 >> 24) & 0xFF)) ^ Td1.get(((_local9 >> 16) & 0xFF))) ^ Td2.get(((_local12 >> 8) & 0xFF))) ^ Td3.get((_local11 & 0xFF))) ^ _arg2.rd_key.get(25));
        _local7 = ((((Td0.get(((_local11 >> 24) & 0xFF)) ^ Td1.get(((_local10 >> 16) & 0xFF))) ^ Td2.get(((_local9 >> 8) & 0xFF))) ^ Td3.get((_local12 & 0xFF))) ^ _arg2.rd_key.get(26));
        _local8 = ((((Td0.get(((_local12 >> 24) & 0xFF)) ^ Td1.get(((_local11 >> 16) & 0xFF))) ^ Td2.get(((_local10 >> 8) & 0xFF))) ^ Td3.get((_local9 & 0xFF))) ^ _arg2.rd_key.get(27));
        _local9 = ((((Td0.get(((_local5 >> 24) & 0xFF)) ^ Td1.get(((_local8 >> 16) & 0xFF))) ^ Td2.get(((_local7 >> 8) & 0xFF))) ^ Td3.get((_local6 & 0xFF))) ^ _arg2.rd_key.get(28));
        _local10 = ((((Td0.get(((_local6 >> 24) & 0xFF)) ^ Td1.get(((_local5 >> 16) & 0xFF))) ^ Td2.get(((_local8 >> 8) & 0xFF))) ^ Td3.get((_local7 & 0xFF))) ^ _arg2.rd_key.get(29));
        _local11 = ((((Td0.get(((_local7 >> 24) & 0xFF)) ^ Td1.get(((_local6 >> 16) & 0xFF))) ^ Td2.get(((_local5 >> 8) & 0xFF))) ^ Td3.get((_local8 & 0xFF))) ^ _arg2.rd_key.get(30));
        _local12 = ((((Td0.get(((_local8 >> 24) & 0xFF)) ^ Td1.get(((_local7 >> 16) & 0xFF))) ^ Td2.get(((_local6 >> 8) & 0xFF))) ^ Td3.get((_local5 & 0xFF))) ^ _arg2.rd_key.get(31));
        _local5 = ((((Td0.get(((_local9 >> 24) & 0xFF)) ^ Td1.get(((_local12 >> 16) & 0xFF))) ^ Td2.get(((_local11 >> 8) & 0xFF))) ^ Td3.get((_local10 & 0xFF))) ^ _arg2.rd_key.get(32));
        _local6 = ((((Td0.get(((_local10 >> 24) & 0xFF)) ^ Td1.get(((_local9 >> 16) & 0xFF))) ^ Td2.get(((_local12 >> 8) & 0xFF))) ^ Td3.get((_local11 & 0xFF))) ^ _arg2.rd_key.get(33));
        _local7 = ((((Td0.get(((_local11 >> 24) & 0xFF)) ^ Td1.get(((_local10 >> 16) & 0xFF))) ^ Td2.get(((_local9 >> 8) & 0xFF))) ^ Td3.get((_local12 & 0xFF))) ^ _arg2.rd_key.get(34));
        _local8 = ((((Td0.get(((_local12 >> 24) & 0xFF)) ^ Td1.get(((_local11 >> 16) & 0xFF))) ^ Td2.get(((_local10 >> 8) & 0xFF))) ^ Td3.get((_local9 & 0xFF))) ^ _arg2.rd_key.get(35));
        _local9 = ((((Td0.get(((_local5 >> 24) & 0xFF)) ^ Td1.get(((_local8 >> 16) & 0xFF))) ^ Td2.get(((_local7 >> 8) & 0xFF))) ^ Td3.get((_local6 & 0xFF))) ^ _arg2.rd_key.get(36));
        _local10 = ((((Td0.get(((_local6 >> 24) & 0xFF)) ^ Td1.get(((_local5 >> 16) & 0xFF))) ^ Td2.get(((_local8 >> 8) & 0xFF))) ^ Td3.get((_local7 & 0xFF))) ^ _arg2.rd_key.get(37));
        _local11 = ((((Td0.get(((_local7 >> 24) & 0xFF)) ^ Td1.get(((_local6 >> 16) & 0xFF))) ^ Td2.get(((_local5 >> 8) & 0xFF))) ^ Td3.get((_local8 & 0xFF))) ^ _arg2.rd_key.get(38));
        _local12 = ((((Td0.get(((_local8 >> 24) & 0xFF)) ^ Td1.get(((_local7 >> 16) & 0xFF))) ^ Td2.get(((_local6 >> 8) & 0xFF))) ^ Td3.get((_local5 & 0xFF))) ^ _arg2.rd_key.get(39));
        if (_arg2.rounds > 10) {
            _local5 = ((((Td0.get(((_local9 >> 24) & 0xFF)) ^ Td1.get(((_local12 >> 16) & 0xFF))) ^ Td2.get(((_local11 >> 8) & 0xFF))) ^ Td3.get((_local10 & 0xFF))) ^ _arg2.rd_key.get(40));
            _local6 = ((((Td0.get(((_local10 >> 24) & 0xFF)) ^ Td1.get(((_local9 >> 16) & 0xFF))) ^ Td2.get(((_local12 >> 8) & 0xFF))) ^ Td3.get((_local11 & 0xFF))) ^ _arg2.rd_key.get(41));
            _local7 = ((((Td0.get(((_local11 >> 24) & 0xFF)) ^ Td1.get(((_local10 >> 16) & 0xFF))) ^ Td2.get(((_local9 >> 8) & 0xFF))) ^ Td3.get((_local12 & 0xFF))) ^ _arg2.rd_key.get(42));
            _local8 = ((((Td0.get(((_local12 >> 24) & 0xFF)) ^ Td1.get(((_local11 >> 16) & 0xFF))) ^ Td2.get(((_local10 >> 8) & 0xFF))) ^ Td3.get((_local9 & 0xFF))) ^ _arg2.rd_key.get(43));
            _local9 = ((((Td0.get(((_local5 >> 24) & 0xFF)) ^ Td1.get(((_local8 >> 16) & 0xFF))) ^ Td2.get(((_local7 >> 8) & 0xFF))) ^ Td3.get((_local6 & 0xFF))) ^ _arg2.rd_key.get(44));
            _local10 = ((((Td0.get(((_local6 >> 24) & 0xFF)) ^ Td1.get(((_local5 >> 16) & 0xFF))) ^ Td2.get(((_local8 >> 8) & 0xFF))) ^ Td3.get((_local7 & 0xFF))) ^ _arg2.rd_key.get(45));
            _local11 = ((((Td0.get(((_local7 >> 24) & 0xFF)) ^ Td1.get(((_local6 >> 16) & 0xFF))) ^ Td2.get(((_local5 >> 8) & 0xFF))) ^ Td3.get((_local8 & 0xFF))) ^ _arg2.rd_key.get(46));
            _local12 = ((((Td0.get(((_local8 >> 24) & 0xFF)) ^ Td1.get(((_local7 >> 16) & 0xFF))) ^ Td2.get(((_local6 >> 8) & 0xFF))) ^ Td3.get((_local5 & 0xFF))) ^ _arg2.rd_key.get(47));
            if (_arg2.rounds > 12) {
                _local5 = ((((Td0.get(((_local9 >> 24) & 0xFF)) ^ Td1.get(((_local12 >> 16) & 0xFF))) ^ Td2.get(((_local11 >> 8) & 0xFF))) ^ Td3.get((_local10 & 0xFF))) ^ _arg2.rd_key.get(48));
                _local6 = ((((Td0.get(((_local10 >> 24) & 0xFF)) ^ Td1.get(((_local9 >> 16) & 0xFF))) ^ Td2.get(((_local12 >> 8) & 0xFF))) ^ Td3.get((_local11 & 0xFF))) ^ _arg2.rd_key.get(49));
                _local7 = ((((Td0.get(((_local11 >> 24) & 0xFF)) ^ Td1.get(((_local10 >> 16) & 0xFF))) ^ Td2.get(((_local9 >> 8) & 0xFF))) ^ Td3.get((_local12 & 0xFF))) ^ _arg2.rd_key.get(50));
                _local8 = ((((Td0.get(((_local12 >> 24) & 0xFF)) ^ Td1.get(((_local11 >> 16) & 0xFF))) ^ Td2.get(((_local10 >> 8) & 0xFF))) ^ Td3.get((_local9 & 0xFF))) ^ _arg2.rd_key.get(51));
                _local9 = ((((Td0.get(((_local5 >> 24) & 0xFF)) ^ Td1.get(((_local8 >> 16) & 0xFF))) ^ Td2.get(((_local7 >> 8) & 0xFF))) ^ Td3.get((_local6 & 0xFF))) ^ _arg2.rd_key.get(52));
                _local10 = ((((Td0.get(((_local6 >> 24) & 0xFF)) ^ Td1.get(((_local5 >> 16) & 0xFF))) ^ Td2.get(((_local8 >> 8) & 0xFF))) ^ Td3.get((_local7 & 0xFF))) ^ _arg2.rd_key.get(53));
                _local11 = ((((Td0.get(((_local7 >> 24) & 0xFF)) ^ Td1.get(((_local6 >> 16) & 0xFF))) ^ Td2.get(((_local5 >> 8) & 0xFF))) ^ Td3.get((_local8 & 0xFF))) ^ _arg2.rd_key.get(54));
                _local12 = ((((Td0.get(((_local8 >> 24) & 0xFF)) ^ Td1.get(((_local7 >> 16) & 0xFF))) ^ Td2.get(((_local6 >> 8) & 0xFF))) ^ Td3.get((_local5 & 0xFF))) ^ _arg2.rd_key.get(55));
            }
        }
        _local4 = (_arg2.rounds << 2);
        _local5 = (((((Td4.get(((_local9 >> 24) & 0xFF)) & 4278190080L) ^ (Td4.get(((_local12 >> 16) & 0xFF)) & 0xFF0000)) ^ (Td4.get(((_local11 >> 8) & 0xFF)) & 0xFF00)) ^ (Td4.get((_local10 & 0xFF)) & 0xFF)) ^ _arg2.rd_key.get(_local4));
        String _local13 = byte2hex(((_local5 >> 24) & 0xFF));
        _local13 = (_local13 + byte2hex(((_local5 >> 16) & 0xFF)));
        _local13 = (_local13 + byte2hex(((_local5 >> 8) & 0xFF)));
        _local13 = (_local13 + byte2hex((_local5 & 0xFF)));
        _local6 = (((((Td4.get(((_local10 >> 24) & 0xFF)) & 4278190080L) ^ (Td4.get(((_local9 >> 16) & 0xFF)) & 0xFF0000)) ^ (Td4.get(((_local12 >> 8) & 0xFF)) & 0xFF00)) ^ (Td4.get((_local11 & 0xFF)) & 0xFF)) ^ _arg2.rd_key.get(_local4 + 1));
        _local13 = (_local13 + byte2hex(((_local6 >> 24) & 0xFF)));
        _local13 = (_local13 + byte2hex(((_local6 >> 16) & 0xFF)));
        _local13 = (_local13 + byte2hex(((_local6 >> 8) & 0xFF)));
        _local13 = (_local13 + byte2hex((_local6 & 0xFF)));
        _local7 = (((((Td4.get(((_local11 >> 24) & 0xFF)) & 4278190080L) ^ (Td4.get(((_local10 >> 16) & 0xFF)) & 0xFF0000)) ^ (Td4.get(((_local9 >> 8) & 0xFF)) & 0xFF00)) ^ (Td4.get((_local12 & 0xFF)) & 0xFF)) ^ _arg2.rd_key.get(_local4 + 2));
        _local13 = (_local13 + byte2hex(((_local7 >> 24) & 0xFF)));
        _local13 = (_local13 + byte2hex(((_local7 >> 16) & 0xFF)));
        _local13 = (_local13 + byte2hex(((_local7 >> 8) & 0xFF)));
        _local13 = (_local13 + byte2hex((_local7 & 0xFF)));
        _local8 = (((((Td4.get(((_local12 >> 24) & 0xFF)) & 4278190080L) ^ (Td4.get(((_local11 >> 16) & 0xFF)) & 0xFF0000)) ^ (Td4.get(((_local10 >> 8) & 0xFF)) & 0xFF00)) ^ (Td4.get((_local9 & 0xFF)) & 0xFF)) ^ _arg2.rd_key.get(_local4 + 3));
        _local13 = (_local13 + byte2hex(((_local8 >> 24) & 0xFF)));
        _local13 = (_local13 + byte2hex(((_local8 >> 16) & 0xFF)));
        _local13 = (_local13 + byte2hex(((_local8 >> 8) & 0xFF)));
        _local13 = (_local13 + byte2hex((_local8 & 0xFF)));
        return (_local13);
    }

    public static Thing AK() {
        Thing thing = new Thing();
        thing.rounds = 12;
        thing.rd_key = new HashMap<Integer, Long>();
        return thing;
    }

    public static String __() {
        return ("1ba011e9b0");
    }

    public static String h2s(String _arg1) {
        String _local2 = "";
        if ((_arg1.length() % 2) == 1) {
            return null;
        }
        int _local3 = 0;
        while (_local3 < _arg1.length()) {
            _local2 = _local2 + (char) Integer.parseInt(_arg1.substring(_local3, _local3 + 2), 16);
            _local3 = (_local3 + 2);
        }
        return (_local2);
    }

    public static String _0(LinkedList<String> _args) {
        //recursive obfuscation crap
        if (_args != null) {
            if (_args.size() > 0) {
                return ((_args.removeFirst() + _0(_args)));
            }
            return "";
        }
        S.copyrighted_strings.addFirst(_0(_1()));
        return "";
    }

    public static LinkedList<String> _1() {
        LinkedList<String> list = new LinkedList<String>();
        list.add(RD._YBAJiRcs());
        list.add(__());
        list.add(___());
        list.add(_());
        return list;
    }

    /* This is a nice one... returns "copyrighted_strings"
    public static String o(){
        int[] _local2 = {0, 12, 13, 22, 15, 6, 4, 5, 17, 2, 1, -4, 16, 17, 15, 6, 11, 4, 16};
        String _local3 = "";
        int _local4 = (_local2.length - 1);
        while (_local4 >= 0) {
            int _temp1 = _local4;
            _local4 = (_local4 - 1);
            _local3 = (char)((99 + _local2[_temp1])) + _local3;
        };
        return (_local3);
    }
    */

    public static String byte2hex(long _arg1) {
        if (Long.toHexString(_arg1).length() < 2) {
            return "0" + Long.toHexString(_arg1).toUpperCase(Locale.ENGLISH);
        }
        return Long.toHexString(_arg1).toUpperCase(Locale.ENGLISH);
    }

    public static void sdk(String _arg1, int _arg2, Thing _arg3) {
        long _local4;
        int _local5;
        int _local6;
        int _local7 = 0;
        int _local8 = sek(_arg1, _arg2, _arg3);
        if (_local8 < 0) {
            return;
        }
        _local5 = 0;
        _local6 = (4 * _arg3.rounds);
        while (_local5 < _local6) {
            _local4 = _arg3.rd_key.get(_local5);
            _arg3.rd_key.put(_local5, _arg3.rd_key.get(_local6));
            _arg3.rd_key.put(_local6, _local4);
            _local4 = _arg3.rd_key.get(_local5 + 1);
            _arg3.rd_key.put(_local5 + 1, _arg3.rd_key.get(_local6 + 1));
            _arg3.rd_key.put(_local6 + 1, _local4);
            _local4 = _arg3.rd_key.get(_local5 + 2);
            _arg3.rd_key.put(_local5 + 2, _arg3.rd_key.get(_local6 + 2));
            _arg3.rd_key.put(_local6 + 2, _local4);
            _local4 = _arg3.rd_key.get(_local5 + 3);
            _arg3.rd_key.put(_local5 + 3, _arg3.rd_key.get(_local6 + 3));
            _arg3.rd_key.put(_local6 + 3, _local4);
            _local5 = (_local5 + 4);
            _local6 = (_local6 - 4);
        }
        _local5 = 1;
        while (_local5 < _arg3.rounds) {
            _local7 = (_local7 + 4);
            _arg3.rd_key.put(_local7, (((Td0.get((Te4.get(((_arg3.rd_key.get(_local7) >> 24) & 0xFF)) & 0xFF)) ^ Td1.get((Te4.get(((_arg3.rd_key.get(_local7) >> 16) & 0xFF)) & 0xFF))) ^ Td2.get((Te4.get(((_arg3.rd_key.get(_local7) >> 8) & 0xFF)) & 0xFF))) ^ Td3.get((Te4.get((_arg3.rd_key.get(_local7) & 0xFF)) & 0xFF))));
            _arg3.rd_key.put(1 + _local7, (((Td0.get((Te4.get(((_arg3.rd_key.get(1 + _local7) >> 24) & 0xFF)) & 0xFF)) ^ Td1.get((Te4.get(((_arg3.rd_key.get(1 + _local7) >> 16) & 0xFF)) & 0xFF))) ^ Td2.get((Te4.get(((_arg3.rd_key.get(1 + _local7) >> 8) & 0xFF)) & 0xFF))) ^ Td3.get((Te4.get((_arg3.rd_key.get(1 + _local7)) & 0xFF)) & 0xFF)));
            _arg3.rd_key.put(2 + _local7, (((Td0.get((Te4.get(((_arg3.rd_key.get(2 + _local7) >> 24) & 0xFF)) & 0xFF)) ^ Td1.get((Te4.get(((_arg3.rd_key.get(2 + _local7) >> 16) & 0xFF)) & 0xFF))) ^ Td2.get((Te4.get(((_arg3.rd_key.get(2 + _local7) >> 8) & 0xFF)) & 0xFF))) ^ Td3.get((Te4.get((_arg3.rd_key.get(2 + _local7)) & 0xFF)) & 0xFF)));
            _arg3.rd_key.put(3 + _local7, (((Td0.get((Te4.get(((_arg3.rd_key.get(3 + _local7) >> 24) & 0xFF)) & 0xFF)) ^ Td1.get((Te4.get(((_arg3.rd_key.get(3 + _local7) >> 16) & 0xFF)) & 0xFF))) ^ Td2.get((Te4.get(((_arg3.rd_key.get(3 + _local7) >> 8) & 0xFF)) & 0xFF))) ^ Td3.get((Te4.get((_arg3.rd_key.get(3 + _local7)) & 0xFF)) & 0xFF)));
            _local5++;
        }
    }

    /*public static LinkedList<Integer> c9(String _arg1) {
        LinkedList<Integer> _local2 = new LinkedList<Integer>();
        int _local3 = 0;
        while (_local3 < (_arg1.length() / 2)) {
            _local2.add(Integer.parseInt(_arg1.substring(2 * _local3, (2 * _local3) + 2), 16));
            _local3++;
        }
        return (_local2);
    }*/

}

class S {

    public static LinkedList<String> copyrighted_strings = new LinkedList<String>();

    static {
        copyrighted_strings.add("c402fb2f70c89a0df112c5e38583f9202a96c6de3fa1aa3da6849bb317a983b3");
        copyrighted_strings.add("e1a28374f5562768c061f22394a556a75860f132432415d67768e0c112c31495");
        copyrighted_strings.add("d3802c10649503a60619b709d1278efef84c1856dfd4097541d55c6740442d8b");
        copyrighted_strings.add("Copyright (c) 2007, 2008, 2009 Hulu, LLC.  All Rights Reserved.");
    }

    /*public static String[] xmldecskeys = [{
        a2:"8CE8829F908C2DFAB8B3407A551CB58EBC19B07F535651A37EBC30DEC33F76A2",
        a3:"O3r9EAcyEeWlm5yV"
    }, {
        a2:"246DB3463FC56FDBAD60148057CB9055A647C13C02C64A5ED4A68F81AE991BF5",
        a3:"vyf8PvpfXZPjc7B1"
    }, {
        a2:"4878B22E76379B55C962B18DDBC188D82299F8F52E3E698D0FAF29A40ED64B21",
        a3:"WA7hap7AGUkevuth"
    }];*/

    /*public static String decs(String _arg1, String _arg2, String _arg3){
        String _local7;
        int _local9;
        Thing _local4 = R.AK();
        R.sdk(_arg2, 0x0100, _local4);
        String _local5 = "";
        LinkedList<Integer> _local6;
        LinkedList<Integer> _local8 = new LinkedList<Integer>();
        _local9 = 0;
        while (_local9 < _arg3.length()) {
            _local8.add((int)_arg3.charAt(_local9));
            _local9++;
        }
        int _local10 = 0;
        while (_local10 < Math.ceil((_arg1.length() / 32))) {
            _local7 = _arg1.substring((_local10 * 32), ((_local10 + 1) * 32));
            _local6 = R.c9(R.e(_local7, _local4));
            _local9 = 0;
            while (_local9 < _local8.size()) {
                _local5 = _local5 + (char)(_local6.get(_local9) ^ _local8.get(_local9));
                _local9++;
            }
            _local8 = R.c9(_local7);
            _local10++;
        }
        return (_local5);
    }*/

    public static String dec(String _arg1) {
        String _local3;
        if (_arg1.indexOf("~") < 0) {
            return (_arg1);
        }
        int _local2 = 0;
        while (_local2 < (S.copyrighted_strings.size() - 1)) {
            _local3 = S.d(_arg1, S.copyrighted_strings.get(_local2));
            if (S.c(_local3)) {
                return (_local3);
            }
            _local2++;
        }
        return (_arg1);
    }

    public static boolean c(String _arg1) {
        int _local3;
        boolean _local4;
        int _local2 = 0;
        while (_local2 < _arg1.length()) {
            _local3 = _arg1.charAt(_local2);
            _local4 = ((('a' <= _local3)) && ((_local3 <= 'z')));
            _local4 = ((_local4) || (((('A' <= _local3)) && ((_local3 <= 'Z')))));
            _local4 = ((_local4) || (((('0' <= _local3)) && ((_local3 <= '9')))));
            _local4 = ((((((_local4) || (('-' == _local3)))) || (('_' == _local3)))) || ((' ' == _local3)));
            if (!_local4) {
                return (false);
            }
            _local2++;
        }
        return (true);
    }

    public static String d(String _arg1, String _arg2) {
        String[] _local3 = _arg1.split("~");
        Thing _local4 = R.AK();
        R.sdk(_local3[1], 0x0100, _local4);
        String[] _local5 = {R.e(_local3[0].substring(0, 32), _local4), R.e(_local3[0].substring(32), _local4)};
        _local4 = R.AK();
        R.sdk(_arg2.substring(0, 64), 0x0100, _local4);
        _local5 = new String[]{R.e(_local5[0], _local4), R.e(_local5[1], _local4)};
        return R.h2s(_local5[0]) + R.h2s(_local5[1]);
    }

    public static String v() {
        return ("101432431");
    }

    /*public static String xmldecs(String _arg1, String _arg2){
        String _local4 = "";
        String _local6;
        String _local7;
        //trace("S.xmldecs ... ");
        if (S.decs == null){
            return (_arg1);
        };
        String _local3 = _arg1;
        int _local5 = 0;
        while (_local5 < xmldecskeys.length) {
            _local6 = xmldecskeys[_local5].a2;
            _local7 = xmldecskeys[_local5].a3;
            _local4 = S.decs(_local3, _local6, _local7);
            if (_local4 != null){
                if (((0) && (!((_arg2 == null))))){
                } else {
                    if (new xml(_local4).firstchild.firstchild != null){
                        //trace("S.xmldecs ... check firstChild succeeded");
                        break;
                    };
                };
            };
            _local5++;
        };
        return (_local4);
    }*/

}
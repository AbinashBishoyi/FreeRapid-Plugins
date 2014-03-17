package cz.vity.freerapid.plugins.services.rtmp;

import java.util.ArrayList;
import java.util.List;

/* xxtea.java
 *
 * Author:       Ma Bingyao < andot@ujn.edu.cn >
 * Copyright:    CoolCode.CN
 * Version:      1.0
 * LastModified: 2006-05-11
 * This library is free.  You can redistribute it and/or modify it.
 * http://www.coolcode.cn/?p=169
 */
public class XXTEA {

   /**
    * Encrypt data with key.
    *
    * @param data
    * @param key
    * @return
    */
   public static byte[] encrypt(byte[] data, byte[] key) {
      if (data.length == 0) {
         return data;
      }
      return toByteArray(encrypt(toIntArray(data, true), toIntArray(key, false)), false);
   }

   private static byte[] hexToChars(String hex) {
      List<Integer> codes = new ArrayList<Integer>();
      for (int i = (hex.substring(0, 2).equals("0x")) ? 2 : 0; i < hex.length(); i += 2) {
         codes.add(Integer.parseInt(hex.substring(i, i + 2), 16));
      }
      byte ret[] = new byte[codes.size()];
      for (int r = 0; r < codes.size(); r++) {
         ret[r] = (byte) codes.get(r).intValue();
      }
      return ret;
   }

   /**
    * Decrypts data with key
    * @author JPEXS
    * @param data Hex encoded data
    * @param key Key as String
    * @return Decrypted data
    */
   public static String decrypt(String data, String key) {
      return new String(decrypt(hexToChars(data), key.getBytes()));
   }

   /**
    * Decrypt data with key.
    *
    * @param data
    * @param key
    * @return
    */
   public static byte[] decrypt(byte[] data, byte[] key) {
      if (data.length == 0) {
         return data;
      }
      return toByteArray(decrypt(toIntArray(data, false), toIntArray(key, false)), false);
   }

   /**
    * Encrypt data with key.
    *
    * @param v
    * @param k
    * @return
    */
   public static int[] encrypt(int[] v, int[] k) {
      int n = v.length - 1;
      if (n < 1) {
         return v;
      }
      if (k.length < 4) {
         int[] key = new int[4];
         System.arraycopy(k, 0, key, 0, k.length);
         k = key;
      }
      int z = v[ n], y = v[ 0], delta = 0x9E3779B9, sum = 0, e;
      int p, q = 6 + 52 / (n + 1);
      while (q-- > 0) {
         sum = sum + delta;
         e = sum >>> 2 & 3;
         for (p = 0; p < n; p++) {
            y = v[ p + 1];
            z = v[ p] += (z >>> 5 ^ y << 2) + (y >>> 3 ^ z << 4) ^ (sum ^ y) + (k[ p & 3 ^ e] ^ z);
         }
         y = v[ 0];
         z = v[ n] += (z >>> 5 ^ y << 2) + (y >>> 3 ^ z << 4) ^ (sum ^ y) + (k[ p & 3 ^ e] ^ z);
      }
      return v;
   }

   /**
    * Decrypt data with key.
    *
    * @param v
    * @param k
    * @return
    */
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
      int z = v[ n], y = v[ 0], delta = 0x9E3779B9, sum, e;
      int p, q = 6 + 52 / (n + 1);
      sum = q * delta;
      while (sum != 0) {
         e = sum >>> 2 & 3;
         for (p = n; p > 0; p--) {
            z = v[ p - 1];
            y = v[ p] -= (z >>> 5 ^ y << 2) + (y >>> 3 ^ z << 4) ^ (sum ^ y) + (k[ p & 3 ^ e] ^ z);
         }
         z = v[ n];
         y = v[ 0] -= (z >>> 5 ^ y << 2) + (y >>> 3 ^ z << 4) ^ (sum ^ y) + (k[ p & 3 ^ e] ^ z);
         sum = sum - delta;
      }
      return v;
   }

   /**
    * Convert byte array to int array.
    *
    * @param data
    * @param includeLength
    * @return
    */
   private static int[] toIntArray(byte[] data, boolean includeLength) {
      int n = (((data.length & 3) == 0) ? (data.length >>> 2)
              : ((data.length >>> 2) + 1));
      int[] result;
      if (includeLength) {
         result = new int[n + 1];
         result[ n] = data.length;
      } else {
         result = new int[n];
      }
      n = data.length;
      for (int i = 0; i < n; i++) {
         result[ i >>> 2] |= (0x000000ff & data[ i]) << ((i & 3) << 3);
      }
      return result;
   }

   /**
    * Convert int array to byte array.
    *
    * @param data
    * @param includeLength
    * @return
    */
   private static byte[] toByteArray(int[] data, boolean includeLength) {
      int n;
      if (includeLength) {
         n = data[ data.length - 1];

      } else {
         n = data.length << 2;
      }

      byte[] result = new byte[n];
      for (int i = 0; i < n; i++) {
         result[ i] = (byte) (data[ i >>> 2] >>> ((i & 3) << 3));
      }
      return result;
   }
}

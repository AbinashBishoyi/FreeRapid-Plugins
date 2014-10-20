package cz.vity.freerapid.plugins.services.canalbrasilglobo;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * @author tong2shot
 */

/*
 Related files from http://s.videos.globo.com/p2/swf/20141014190539/player.swf:
 com.globo.player.security.MD5 -> pay attention to PADDING
 com.globo.player.security.OnDemandHashSigner
 com.globo.player.security.HashSigner
 */
class Crypto {
    private final static int RESIGN_EXPIRATION = 86400; //24 * 60 * 60
    private final static int RECEIVED_TIME_POSITION = 2;
    private final static int RECEIVED_RANDOM_POSITION = 12;
    private final static int RECEIVED_MD5_POSITION = 22;
    private final static String PADDING = "0xFF01DD"; //"=0xFF01DD".substring(1,9)

    private final String hash;

    public Crypto(String hash) {
        this.hash = hash;
    }

    private String hashVersion() {
        return extract(0, 2);
    }

    private String receivedTime() {
        return extract(RECEIVED_TIME_POSITION, 10);
    }

    private String receivedRandom() {
        return extract(RECEIVED_RANDOM_POSITION, 10);
    }


    private String receivedMD5() {
        return extract(RECEIVED_MD5_POSITION, 22);
    }

    private String randomPadding() {
        String strRandom = String.valueOf(Math.round(Math.random() * 1.0E10));
        if (strRandom.length() < 10) {
            for (int i = 0, strRandomLength = strRandom.length(); i < (10 - strRandomLength); i++) {
                strRandom = "0" + strRandom;
            }
        }
        return strRandom;
    }

    private String signTime() {
        return String.valueOf(Integer.parseInt(receivedTime()) + RESIGN_EXPIRATION);
    }

    private String extract(int param1, int param2) {
        return hash.substring(param1, param2 + param1);
    }

    public String sign() {
        String _loc1_ = signTime();
        String _loc2_ = randomPadding();
        String receivedMD5 = receivedMD5();
        String _loc3_ = Base64.encodeBase64String(DigestUtils.md5(receivedMD5 + _loc1_ + _loc2_ + PADDING)).replace("+", "-").replace("/", "_").replaceFirst("==?$", "");
        String hashVersion = hashVersion();
        String receivedTime = receivedTime();
        String receivedRandom = receivedRandom();
        return hashVersion + receivedTime + receivedRandom + _loc1_ + _loc2_ + _loc3_;
    }
}

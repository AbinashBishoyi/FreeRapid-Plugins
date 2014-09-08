package cz.vity.freerapid.plugins.services.videomeh;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import org.codehaus.jackson.JsonNode;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author tong2shot
 */
class Crypto {
    /*
     Related files from p.swf :
     /com/videobb/kageo/kCryptoString.as
     /com/videobb/kageo/vbCryptoSelector.as
     */

    private final static Logger logger = Logger.getLogger(Crypto.class.getName());

    public String parse(JsonNode rootNode) throws PluginImplementationException {
        String keyString1;
        int randomKey;
        String decryptedString;
        int outputInternalKey;
        String[] tempKeyValue;
        String[] paramNameType;
        String output = "";
        try {
            String outputInfoRAW = rootNode.get("settings").get("login_status").get("spen").getTextValue();
            int outputInfoKey1 = rootNode.get("settings").get("login_status").get("salt").getIntValue();
            int outputInfoKey2 = 950569;
            String outputInfo = hex2str(bitDecrypt(outputInfoRAW, outputInfoKey1, outputInfoKey2, false));
            logger.info("OutputInfo: " + outputInfo);
            String[] outputInfoSet = outputInfo.split(";");
            String outputPattern = outputInfoSet[0];
            String outputKeySet = outputInfoSet[1];
            Map<String, Integer> outputKeys = new HashMap<String, Integer>();
            for (String eachKeyValue : outputKeySet.split("&")) {
                tempKeyValue = eachKeyValue.split("=");
                outputKeys.put(tempKeyValue[0], Integer.parseInt(tempKeyValue[1]));
            }
            outputInternalKey = getInternalKey(outputKeys.get("ik"));
            for (String eachValue : outputPattern.split("&")) {
                paramNameType = eachValue.split("=");
                switch (Integer.parseInt(paramNameType[1])) {
                    case 1:
                        keyString1 = rootNode.get("settings").get("video_details").get("sece2").getTextValue();
                        randomKey = rootNode.get("settings").get("config").get("rkts").getIntValue();
                        decryptedString = bitDecrypt(keyString1, randomKey, outputInternalKey, true);
                        output = output + (paramNameType[0] + "=" + decryptedString + "&");
                        continue;
                    case 2:
                        keyString1 = rootNode.get("settings").get("banner").get("g_ads").get("url").getTextValue();
                        randomKey = rootNode.get("settings").get("config").get("rkts").getIntValue();
                        decryptedString = bitDecrypt(keyString1, randomKey, outputInternalKey, false);
                        output = output + (paramNameType[0] + "=" + decryptedString + "&");
                        continue;
                    case 3:
                        keyString1 = rootNode.get("settings").get("banner").get("g_ads").get("type").getTextValue();
                        randomKey = rootNode.get("settings").get("config").get("rkts").getIntValue();
                        decryptedString = d9300(keyString1, randomKey, outputInternalKey);
                        output = output + (paramNameType[0] + "=" + decryptedString + "&");
                        continue;
                    case 4:
                        keyString1 = rootNode.get("settings").get("banner").get("g_ads").get("time").getTextValue();
                        randomKey = rootNode.get("settings").get("config").get("rkts").getIntValue();
                        decryptedString = lion(keyString1, randomKey, outputInternalKey);
                        output = output + (paramNameType[0] + "=" + decryptedString + "&");
                        continue;
                    case 5:
                        keyString1 = rootNode.get("settings").get("login_status").get("euno").getTextValue();
                        randomKey = rootNode.get("settings").get("login_status").get("pepper").getIntValue();
                        decryptedString = heal(keyString1, randomKey, outputInternalKey);
                        output = output + (paramNameType[0] + "=" + decryptedString + "&");
                        continue;
                    case 6:
                        keyString1 = rootNode.get("settings").get("login_status").get("sugar").getTextValue();
                        randomKey = rootNode.get("settings").get("banner").get("lightbox2").get("time").getIntValue();
                        decryptedString = brokeup(keyString1, randomKey, outputInternalKey);
                        output = output + (paramNameType[0] + "=" + decryptedString + "&");
                        continue;
                    default:
                }
            }
        } catch (Exception ex) {
            throw new PluginImplementationException("Error parsing player's settings JSON (2)");
        }
        logger.info("Parsing output: " + output);
        return output;
    }

    //(is32byte = true) => decrypt32byte()
    private String bitDecrypt(String param1, int param2, int param3, int param4, int param5, int param6, int param7, int param8, int param9, boolean is32byte) {
        int _loc17_;
        int _loc18_;
        String _loc19_;
        String _loc20_;
        List<String> binList = string2bin(param1);
        int _loc11_ = (!is32byte ? binList.size() * 2 : 256);
        int[] _loc12_ = new int[(int) (_loc11_ * 1.5)];
        for (int i = 0, loopTo = (int) (_loc11_ * 1.5); i < loopTo; i++) {
            param2 = (param2 * param4 + param5) % param6;
            param3 = (param3 * param7 + param8) % param9;
            _loc12_[i] = (param2 + param3) % ((int) (_loc11_ * 0.5));
        }
        for (int i = _loc11_; i >= 0; i--) {
            _loc17_ = _loc12_[i];
            _loc18_ = i % ((int) (_loc11_ * 0.5));
            _loc19_ = binList.get(_loc17_);
            binList.set(_loc17_, binList.get(_loc18_));
            binList.set(_loc18_, _loc19_);
        }
        for (int i = 0, loopTo = (int) (_loc11_ * 0.5); i < loopTo; i++) {
            binList.set(i, String.valueOf(Integer.parseInt(binList.get(i)) ^ _loc12_[i + _loc11_] & 1));
        }

        StringBuilder sb = new StringBuilder(binList.size());
        for (String bin : binList) {
            sb.append(bin);
        }
        String _loc14_ = sb.toString();

        List<String> _loc15_ = new ArrayList<String>();
        for (int i = 0, loopTo = _loc14_.length(); i < loopTo; i += 4) {
            _loc20_ = _loc14_.substring(i, i + 4);
            _loc15_.add(_loc20_);
        }
        return bin2string(_loc15_);
    }

    private String bitDecrypt(String param1, int param2, int param3, boolean is32byte) {
        return bitDecrypt(param1, param2, param3, 11, 77213, 81371, 17, 92717, 192811, is32byte);
    }

    private List<String> string2bin(String param1) {
        StringBuilder sb = new StringBuilder(param1.length());
        for (int i = 0, loopTo = param1.length(); i < loopTo; i++) {
            sb.append(String.format("%04d", Integer.parseInt(Integer.toBinaryString(Integer.parseInt(param1.substring(i, i + 1), 16)))));
        }
        List<String> stringList = new ArrayList<String>(Arrays.asList(sb.toString().split("")));
        if (stringList.get(0).isEmpty()) { //JRE < 8 regex bug (https://bugs.openjdk.java.net/browse/JDK-8027645)
            stringList.remove(0);
        }
        return stringList;
    }

    private String bin2string(List<String> param1) {
        StringBuilder sb = new StringBuilder(param1.size());
        for (String aParam1 : param1) {
            sb.append(Integer.toHexString(Integer.parseInt(aParam1, 2)));
        }
        return sb.toString();
    }

    private String hex2str(String param1) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i = 0, loopTo = param1.length(); i < loopTo; i += 2) {
            baos.write(Integer.parseInt(Character.toString(param1.charAt(i)) + Character.toString(param1.charAt(i + 1)), 16));
        }
        return baos.toString();
    }

    private String d9300(String param1, int param2, int param3) {
        return bitDecrypt(param1, param2, param3, 26, 25431, 56989, 93, 32589, 784152, false);
    }

    private String lion(String param1, int param2, int param3) {
        return bitDecrypt(param1, param2, param3, 82, 84669, 48779, 32, 65598, 115498, false);
    }

    private String heal(String param1, int param2, int param3) {
        return bitDecrypt(param1, param2, param3, 10, 12254, 95369, 39, 21544, 545555, false);
    }

    private String brokeup(String param1, int param2, int param3) {
        return bitDecrypt(param1, param2, param3, 22, 66595, 17447, 52, 66852, 400595, false);
    }

    private int getInternalKey(int param1) {
        switch (param1) {
            case 1:
                return 226593;
            case 2:
                return 441252;
            case 3:
                return 301517;
            case 4:
                return 596338;
            case 5:
                return 852084;
            default:
                return 0;
        }
    }
}

package cz.vity.freerapid.plugins.services.edisk.captcha;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * This is reader for recognizing Edisk sound captcha
 * @author JPEXS
 */
public class SoundReader {

    private final static Logger logger = Logger.getLogger(SoundReader.class.getName());
    private static final byte[][] patterns = new byte[][]{
        {-128, -128, 127, -128, -128, -128},
        {-128, -128, 127, -128, 127, -128},
        {-128, -128, 127, -128, -128, 127},
        {-128, -128, -128, 127, -128, -128},
        {-128, -128, -128, -128, 127, -128},
        {-128, 127, 127, 127, 127, -128},
        null, //6 is not used
        {127, -128, 127, -128, -128, -128},
        {-128, 127, -128, -128, 127, -128},
        {127, -128, -128, -128, -128, -128}
    };
    private static final int lengths[] = new int[]{5750, 9716, 7337, 7337, 8527, 11104, -1, 10509, 9271, 8264};

    public static String readWav(InputStream fis) {
        String ret = "";
        if(fis==null){
            logger.warning("Null inputstream for wav");
        }
        try {
            byte buff[] = new byte[4];
            fis.read(buff);
            String header=new String(buff);
            if(!header.equals("RIFF")){
                logger.warning("Not a wave file");
                return "";
            }
            fis.skip(36);
            
            fis.read(buff);
            int dataSize = ((buff[3] << 24) & 0xFF000000) | ((buff[2] << 16) & 0x00FF0000) | ((buff[1] << 8) & 0x0000FF00) | (buff[0] & 0x000000FF);
            byte data[] = new byte[dataSize];
            for(int d=0;d<dataSize;d++)
                data[d]=(byte)fis.read();
            fis.close();
            int pos = 0;
            loopposition:
            for (int i = 0; i < 4; i++) {
                loopnumbers:
                for (int c = 0; c < 10; c++) {
                    if (c != 6) {
                        for (int k = 0; k < 6; k++) {
                            if (data[pos + k] != patterns[c][k]) {
                                continue loopnumbers;
                            }
                        }
                        ret += "" + c;
                        pos += lengths[c];
                        continue loopposition;
                    }
                }
            }
        } catch (IOException ex) {
            logger.severe("Error reading wav");
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ex) {
                }
            }
        }
        return ret;
    }
}

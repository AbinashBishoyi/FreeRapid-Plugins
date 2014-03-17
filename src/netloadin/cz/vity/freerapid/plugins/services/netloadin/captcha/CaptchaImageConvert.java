package cz.vity.freerapid.plugins.services.netloadin.captcha;

import java.io.*;
import java.util.Arrays;
import java.util.zip.CRC32;

/**
 * This class loads PNG image and changes its palette to black & white
 * according to palette index
 *
 * @author JPEXS
 */
public class CaptchaImageConvert {

    static long width;
    static long height;
    static int bitDepth;
    static int colorType;
    static int compressionMethod;
    static int filterMethod;
    static int interlaceMethod;

    private static long readFour(DataInputStream dis,OutputStream os) throws IOException{
        byte fourbytes[] = new byte[4];
            dis.readFully(fourbytes);
            os.write(fourbytes);
            return ((fourbytes[0] & 0xff) << 24) + ((fourbytes[1] & 0xff) << 16) + ((fourbytes[2] & 0xff) << 8) + ((fourbytes[3] & 0xff));

    }

   public static InputStream convert(InputStream is) throws IOException {
        DataInputStream dis = new DataInputStream(is);
        ByteArrayOutputStream bos=new ByteArrayOutputStream();
        byte correctSignature[] = new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10};
        byte signature[] = new byte[8];
        dis.readFully(signature);
        if (!Arrays.equals(signature, correctSignature)) {
            throw new IOException("Invalid png");
        }
        bos.write(signature);
        long crcChange=-1;
        while (dis.available() > 0) {            
            long chunkLength=readFour(dis,bos);
            byte type[] = new byte[4];
            dis.readFully(type);
            bos.write(type);
            String typeStr = new String(type);
            if (typeStr.equals("IHDR")) {
                width = readFour(dis, bos);
                height = readFour(dis, bos);
                bitDepth = dis.read(); bos.write(bitDepth);
                colorType = dis.read(); bos.write(colorType);
                compressionMethod = dis.read(); bos.write(compressionMethod);
                filterMethod = dis.read(); bos.write(filterMethod);
                interlaceMethod = dis.read(); bos.write(interlaceMethod);
            }else {
                int blackCount=37; //first 37 palette entries will be black (index 0 not included)

                if(typeStr.equals("PLTE")&&(colorType==3)&&(bitDepth==8)){
                    CRC32 crc=new CRC32();
                    crc.update("PLTE".getBytes());


                    for(int i=0;i<chunkLength;i+=3){
                        dis.read(); dis.read(); dis.read();
                        if((i/3<blackCount)&&(i>0)){
                            bos.write(0); bos.write(0); bos.write(0);
                            crc.update(0); crc.update(0); crc.update(0);
                        } else {
                            bos.write(255); bos.write(255); bos.write(255);
                            crc.update(255); crc.update(255); crc.update(255);
                        }
                        crcChange=crc.getValue();
                    }
                }else{
                    for(int i=0;i<chunkLength;i++){
                        bos.write(dis.read());
                    }
                }
            }
            byte crc[] = new byte[4];
            dis.readFully(crc);
            if(crcChange>-1){
                bos.write((int)((crcChange>>24) & 0xff));
                bos.write((int)((crcChange>>16) & 0xff));
                bos.write((int)((crcChange>>8) & 0xff));
                bos.write((int)((crcChange) & 0xff));
                crcChange=-1;
            }else{
                bos.write(crc);
            }
            if(typeStr.equals("IEND"))
                break;
        }
        dis.close();
        bos.close();
        return new ByteArrayInputStream(bos.toByteArray());
    }



}

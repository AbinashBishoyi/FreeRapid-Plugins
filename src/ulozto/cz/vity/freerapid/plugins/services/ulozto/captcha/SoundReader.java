package cz.vity.freerapid.plugins.services.ulozto.captcha;

import javazoom.jl.decoder.*;

import java.io.*;

/**
 * Class for reading Uloz.to sound captcha
 * @author JPEXS
 */

public class SoundReader {

    private DataOutputStream dos;
    private ByteArrayOutputStream bos;
    private int pozice=-1;
    private boolean nuly=true;
    private long pocetNul=0;
    private String word="";
    private static Classifier classifier=null;

    
    public SoundReader() {
       if(classifier==null)
       {
         classifier=new Classifier();
         classifier.load("/resources/captcha.bin");
       }
    }




    public String parse(InputStream stream) throws JavaLayerException {        
        Bitstream bitstream = new Bitstream(stream);
        Decoder decoder = new Decoder();

        boolean ret = true;
        int frames=Integer.MAX_VALUE;

        dos=null;
        bos=null;
        word="";
        pozice=-1;
        while (frames-- > 0 && ret) {
            ret = decodeFrame(bitstream,decoder);
        }
        if(dos!=null){
            try {
                dos.close();
                bos.close();
                word += classifier.ctiStream(new ByteArrayInputStream(bos.toByteArray()));
            } catch (IOException ex) {

            }
                                        }
        nuly=true;
        pocetNul=0;
        return word;
    }

 


    protected boolean decodeFrame(Bitstream bitstream,Decoder decoder) throws JavaLayerException {
        try {

            Header h = bitstream.readFrame();

            if (h == null) {
                return false;
            }

            SampleBuffer output = (SampleBuffer) decoder.decodeFrame(h, bitstream);

                    short[] buf = output.getBuffer();
                    int len = output.getBufferLength();
                    for (int p = 0; p < len; p++) {                        
                        if(nuly){
                            if(buf[p]!=0){
                                nuly=false;
                                if(pocetNul>1000){
                                    pozice++;
                                    if(pozice>3){
                                        return false;
                                    }
                                    try {
                                        if(dos!=null){
                                         dos.flush();
                                         word+=classifier.ctiStream(new ByteArrayInputStream(bos.toByteArray()));
                                         dos.close();
                                         bos.close();
                                        }
                                        bos=new ByteArrayOutputStream();
                                        dos = new DataOutputStream(bos);
                                    } catch (IOException ex) {

                                    }
                                }
                                pocetNul=0;
                            }else{
                                pocetNul++;
                            }
                        }else{
                            if(buf[p]==0){
                                nuly=true;
                                pocetNul++;
                            }
                        }
                        try {
                            if(dos!=null)
                              dos.writeShort(buf[p]);
                        } catch (IOException ex) {

                        }
                    }
                
            

            bitstream.closeFrame();
        } catch (RuntimeException ex) {
            throw new JavaLayerException("Exception decoding audio frame", ex);
        }
        return true;
    }
}

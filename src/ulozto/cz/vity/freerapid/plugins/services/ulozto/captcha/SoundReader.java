package cz.vity.freerapid.plugins.services.ulozto.captcha;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javazoom.jl.decoder.*;
import java.util.List;

import javazoom.jl.decoder.*;

import java.io.*;

/**
 * Class for reading Uloz.to sound captcha
 * @author JPEXS
 */

public class SoundReader {
   Bitstream bitstream;
   ArrayList<Short> list=new ArrayList<Short>();
   Decoder decoder;
   BufferedImage img = null;
   private List<FromTo> parts;
   static Classifier classifier=new Classifier();
   
   static{
      classifier.load("/resources/captcha.bin");
   }
   
   private class FromTo{
      public int from;
      public int to;

      public FromTo(int from, int to) {
         this.from = from;
         this.to = to;
      }

      @Override
      public String toString() {
         return from+"->"+to+" len:"+(to-from);
      }
      
      
   }
    
    private void init(InputStream is){
       boolean ret = true;
      decoder = new Decoder();     
      list.clear();
         bitstream= new Bitstream(is);

		while ( ret)
		{
         try {
            ret = decodeFrame();
         } catch (JavaLayerException ex) {
            
         }
		} 
      try {
         bitstream.close();
      } catch (BitstreamException ex) {
         
      }
      parts=getParts();
    }
    
   public String parse(InputStream is)
   {
      init(is);
      List<SoundPattern> patterns=getPatterns();
      int startpos=0;
      if(patterns.size()>4){
         startpos++;
      }
      String ret="";
      for(int i=startpos;i<patterns.size();i++)
      {
         ret+=classifier.getCharacter(patterns.get(i));
      }
      return ret;
   }

   public List<SoundPattern> getPatterns()
   {
      List<SoundPattern> ret=new ArrayList<SoundPattern>();
      int step=2000;
      for(FromTo ft:parts)
      {
         short data[]=new short[(ft.to-ft.from)/step];
         short max=0;
         int pos=0;
         int dpos=0;
         for(int i=ft.from;i<ft.to;i++)
         {
            short val=(short)Math.abs(list.get(i));
            if(val>max){
               max=val;
            }
            if(pos==step){               
               data[dpos] = max;
               max=0;
               dpos++;               
               pos=0;
            }else{
               pos++;
            }
         }
        
         SoundPattern pat=new SoundPattern('?',data);
         ret.add(pat);
      }
      return ret;
   }
   
    
   private List<FromTo> getParts()
   {
      List<FromTo> ret=new ArrayList<FromTo>();
      int pocetnul=0;
      int lastwidth=0;
      int prah=100;
      int nullimit=200;
      boolean zacatek=true;
      for(int i=0;i<list.size();i++)
      {       
         short val=list.get(i);
         lastwidth++;
         if(Math.abs(val)<prah){
            pocetnul++;
         }else{
            if(zacatek){
               lastwidth=0;
               zacatek=false;
               continue;
            }
            if((pocetnul>nullimit)&&(lastwidth-pocetnul>10000)){
               ret.add(new FromTo(i-lastwidth+1,i-pocetnul));
               lastwidth=0;               
            }
            pocetnul=0;
         }
      }
      ret.add(new FromTo(list.size()-lastwidth,list.size()-1-pocetnul));
      return ret;
   }

private boolean decodeFrame() throws JavaLayerException
	{		

		try
		{
			Header h = bitstream.readFrame();	
			
			if (h==null)
				return false;
				
			// sample buffer set when decoder constructed
			SampleBuffer output = (SampleBuffer)decoder.decodeFrame(h, bitstream);
										
         short buf[]=output.getBuffer();
			for(short s:buf)
         {
            list.add(s);
         }																			
			bitstream.closeFrame();
		}		
		catch (RuntimeException ex)
		{
			throw new JavaLayerException("Exception decoding audio frame", ex);
		}		
		return true;
	}
}
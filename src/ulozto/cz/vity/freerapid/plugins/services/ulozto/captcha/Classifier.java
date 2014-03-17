
package cz.vity.freerapid.plugins.services.ulozto.captcha;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * kNN classifier
 * @author JPEXS
 */
public class Classifier {
    public int k=3;
    public static String allLetters = "abcdefghijklmnopqrstuvxz";

    private class ComComparator implements Comparator{

        public int compare(Object o1, Object o2) {
            if(o1 instanceof Compatibility){
                if(o2 instanceof Compatibility){
                double c1=((Compatibility)o1).comp;
                double c2=((Compatibility)o2).comp;
                if(c1<c2){
                    return -1;
                }
                if(c1>c2){
                    return 1;
                }
                return 0;
            }
            }
            return -1;
        }

    }

    private class Compatibility {
        public SoundPattern vzor;
        public double comp;

        public Compatibility(SoundPattern vzor, double comp) {
            this.vzor = vzor;
            this.comp = comp;
        }

        public int compareTo(Object o) {
            if(o instanceof Compatibility){
                double c2=((Compatibility)o).comp;
                if(comp<c2){
                    return -1;
                }
                if(comp>c2){
                    return 1;
                }
                return 0;
            }
            return -1;
        }

        @Override
        public String toString() {
            return ""+vzor.character+""+vzor.poradi+":"+comp;
        }

    }

    public List<SoundPattern> vzory=new ArrayList<SoundPattern>();
    
    public char getCharacter(SoundPattern ciziVzor){
        double min=Integer.MAX_VALUE;
        ArrayList<Compatibility> cmpList=new ArrayList<Compatibility>();
        for(SoundPattern v:vzory){
            cmpList.add(new Compatibility(v,v.getCompatibility(ciziVzor)));
        }
        Collections.sort(cmpList,new ComComparator());

        int pocty[]=new int[allLetters.length()];
        for(int i=0;i<k;i++){
            Compatibility cp=cmpList.get(i);
           SoundPattern v=cp.vzor;
           pocty[allLetters.indexOf(v.character)]++;
        }
        int max=0;
        int maxIndex=0;
        for(int i=0;i<pocty.length;i++){
            if(pocty[i]>max){
                max=pocty[i];
                maxIndex=i;
            }
        }
        return allLetters.charAt(maxIndex);
    }

    public void load(String fname){
        try {
            DataInputStream dis = new DataInputStream(new InflaterInputStream(this.getClass().getResourceAsStream(fname)));            
            int pocetVzorku=dis.readInt();
            vzory.clear();
            for(int v=0;v<pocetVzorku;v++){             
                char znak=dis.readChar();
                int pocetHodnot=dis.readInt();
                short hodnoty[]=new short[pocetHodnot];
                for(int h=0;h<pocetHodnot;h++){
                    hodnoty[h]=dis.readShort();
                }
                SoundPattern vzor=new SoundPattern(znak, hodnoty);
                vzory.add(vzor);
            }
            dis.close();
        } catch (IOException ex) {

        }
    }

    public void save(String fname){
        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(new DeflaterOutputStream(new FileOutputStream(fname)));
            dos.writeInt(vzory.size());
            for (SoundPattern v : vzory) {
                dos.writeChar(v.character);
                dos.writeInt(v.values.length);
                for(int h=0;h<v.values.length;h++){
                    dos.writeShort(v.values[h]);
                }

            }
        } catch (IOException ex) {

        } finally {
            try {
                dos.close();
            } catch (IOException ex) {

            }
        }
    }
    
}

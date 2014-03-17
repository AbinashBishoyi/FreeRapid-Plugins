
package cz.vity.freerapid.plugins.services.ulozto.captcha;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * kNN classifier
 * @author JPEXS
 */
public class Classifier {
    public int k=3;
    public static int patternHeight = 8;
    public static int patternWidth = 1000;
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
        //System.out.println("");
        //System.out.println("Compat:");
        for(int i=0;i<k;i++){
            Compatibility cp=cmpList.get(i);
           SoundPattern v=cp.vzor;
           // System.out.println(cp);
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
            DataInputStream dis = new DataInputStream(this.getClass().getResourceAsStream(fname));
            patternHeight=dis.readInt();
            patternWidth=dis.readInt();
            int pocetVzorku=dis.readInt();
            vzory.clear();
            for(int v=0;v<pocetVzorku;v++){
                char znak=dis.readChar();
                int pocetHodnot=dis.readInt();
                int hodnoty[]=new int[pocetHodnot];
                for(int h=0;h<pocetHodnot;h++){
                    hodnoty[h]=dis.read();
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
            dos = new DataOutputStream(new FileOutputStream(fname));
            dos.writeInt(patternHeight);
            dos.writeInt(patternWidth);
            dos.writeInt(vzory.size());
            for (SoundPattern v : vzory) {
                dos.writeChar(v.character);
                dos.writeInt(v.values.length);
                for(int h=0;h<v.values.length;h++){
                    dos.write(v.values[h]);
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

    public char ctiStream(InputStream is){
        SoundPattern vz=zjistiVzorZeStreamu(is);
        return getCharacter(vz);
    }

    public static SoundPattern zjistiVzorZeStreamu(InputStream is) {
        DataInputStream dis=null;
        try {
        dis=new DataInputStream(is);
        long delka=dis.available()/2;
        long skip = patternWidth;
         int pocetVzorku = (int) (delka / skip);
        int ret[] = new int[pocetVzorku];



            long pocetNul = 0;
            long sum = 0;
            int sk = 0;
            int pocet = 0;
            int max = 0;
            while (dis.available()>2) {
                int g = Math.abs(dis.readShort());
                if (g > max) {
                    max = g;
                }
                sum += g;
                sk++;
                if (sk < skip) {
                    continue;
                }
                sum = sum / skip;
                ret[pocet] = max * patternHeight / Short.MAX_VALUE;
                sk = 0;
                max = 0;
                sum = 0;
                pocet++;
                if (pocet == pocetVzorku) {
                    break;
                }
            }
            SoundPattern vz = new SoundPattern(' ',ret);
        vz.length = delka;
        return vz;
        } catch (IOException ex) {
        } finally {
            try {
                dis.close();
            } catch (IOException ex) {
            }
        }
        return null;
    }
    
}

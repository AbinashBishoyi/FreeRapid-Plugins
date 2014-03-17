
package cz.vity.freerapid.plugins.services.ulozto.captcha;

/**
 * One sound pattern
 * @author JPEXS
 */
public class SoundPattern {
    public short values[];
    public long length;
    public char character;
    public static int maxShift=1;
    public int poradi=0;

    
    
    
    public SoundPattern(char znak,short[] hodnoty) {
        this.values = hodnoty;
        this.character=znak;
    }

    

    public double getCompatibility(SoundPattern otherPattern){
        short otherValues[]=otherPattern.values;
        double totalDifference=0;
        int minlen=values.length<otherValues.length?values.length:otherValues.length;
        int maxlen=values.length>otherValues.length?values.length:otherValues.length;
        
        totalDifference=100*(maxlen-minlen);
        for(int i=0;i<maxlen;i++) {
            short a;
            if(i>=values.length){
               a=0;
            }else{
               a=values[i];
            }
            short b;
            if(i>=otherValues.length){
               b=0;
            }else{
               b=otherValues[i];
            }
            totalDifference+=Math.abs(a-b);
        }       
        return totalDifference;
    }
}

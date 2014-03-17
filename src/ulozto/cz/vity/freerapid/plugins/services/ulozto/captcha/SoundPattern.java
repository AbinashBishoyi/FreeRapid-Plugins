
package cz.vity.freerapid.plugins.services.ulozto.captcha;

/**
 * One sound pattern
 * @author JPEXS
 */
public class SoundPattern {
    public int values[];
    public long length;
    public char character;
    public static int maxShift=1;
    public int poradi=0;

    public SoundPattern(char znak,int[] hodnoty) {
        this.values = hodnoty;
        this.character=znak;
    }

    

    public double getCompatibility(SoundPattern otherPattern){
        int otherValues[]=otherPattern.values;
        double totalDifference=0;
        int minlen=values.length<otherValues.length?values.length:otherValues.length;
        int maxlen=values.length>otherValues.length?values.length:otherValues.length;
        for(int i=0;i<maxlen;i++) {
            double minDifference=Double.MAX_VALUE;
            for(int r=-maxShift;r<=maxShift;r++){
                double diff;
                if(i+r<0) continue;
                if((i+r>=values.length)&&(i+r>=otherValues.length)) continue;
                if(i+r>=values.length){
                    diff=Math.abs(otherValues[i+r]);
                }else if(i+r>=otherValues.length){
                    diff=Math.abs(values[i+r]);
                }else{
                    diff=(double)(Math.abs(values[i+r])-Math.abs(otherValues[i+r]))/Short.MAX_VALUE;
                }
                if(diff<minDifference) minDifference=diff;
            }
            totalDifference+=Math.abs(minDifference/100);
        }
        return totalDifference;
    }
}

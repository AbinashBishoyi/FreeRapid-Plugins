package cz.vity.freerapid.plugins.services.ceskatelevize;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author JPEXS
 */
public class SwitchItem implements Comparable<SwitchItem>{
    public String base="";
    public double duration;
    public List<Video> videos=new ArrayList<Video>();

    @Override
    public int compareTo(SwitchItem o) {
        return (o.duration-duration)<0?-1:0;
    }

}

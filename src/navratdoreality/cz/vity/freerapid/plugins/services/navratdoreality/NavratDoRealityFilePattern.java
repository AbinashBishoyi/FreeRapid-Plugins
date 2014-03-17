package cz.vity.freerapid.plugins.services.navratdoreality;

/**
 * Created with IntelliJ IDEA.
 * User: A6KT
 * Date: 14.7.12
 * Time: 8:03
 * To change this template use File | Settings | File Templates.
 */
public class NavratDoRealityFilePattern {
    private String before;
    private String after;
    private String center;

    public NavratDoRealityFilePattern(String before, String center, String after){
        this.setBefore(before);
        this.setCenter(center);
        this.setAfter(after);
    }

    public String getFullRegexp(){
        return this.getBefore() + this.getCenter() + this.getAfter();
    }

    public String getAfter() {
        return after;
    }

    public void setAfter(String after) {
        this.after = after;
    }

    public String getBefore() {
        return before;
    }

    public void setBefore(String before) {
        this.before = before;
    }

    public String getCenter() {
        return center;
    }

    public void setCenter(String center) {
        this.center = center;
    }
}

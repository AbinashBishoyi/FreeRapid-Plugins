package cz.vity.freerapid.plugins.services.rapidshare;

import java.util.ArrayList;


public class RapidShareMirrorConfig {
    ArrayList<MirrorBean> ar;
   String chosen;

    public ArrayList<MirrorBean> getAr() {
        return ar;
    }

    public void setAr(ArrayList<MirrorBean> ar) {
        this.ar = ar;
    }

    public String getChosen() {
        return chosen;
    }

    public void setChosen(String chosen) {
        this.chosen = chosen;
    }
}


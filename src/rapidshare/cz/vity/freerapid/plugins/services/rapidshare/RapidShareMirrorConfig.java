package cz.vity.freerapid.plugins.services.rapidshare;

import java.util.ArrayList;


public class RapidShareMirrorConfig {
    private ArrayList<MirrorBean> ar;
    private MirrorBean chosen;

    public ArrayList<MirrorBean> getAr() {
        return ar;
    }

    public void setAr(ArrayList<MirrorBean> ar) {
        this.ar = ar;
    }

    public MirrorBean getChosen() {
        return chosen;
    }

    public void setChosen(MirrorBean chosen) {
        this.chosen = chosen;
    }
}


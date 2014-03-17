package cz.vity.freerapid.plugins.services.rapidshare;

import java.util.ArrayList;


public class RapidShareMirrorConfig {
    ArrayList<MirrorBean> ar;
   String choosen;

    public ArrayList<MirrorBean> getAr() {
        return ar;
    }

    public void setAr(ArrayList<MirrorBean> ar) {
        this.ar = ar;
    }

    public String getChoosen() {
        return choosen;
    }

    public void setChoosen(String choosen) {
        this.choosen = choosen;
    }
}


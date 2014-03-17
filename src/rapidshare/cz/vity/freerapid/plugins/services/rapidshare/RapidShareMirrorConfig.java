package cz.vity.freerapid.plugins.services.rapidshare;

import java.util.Collections;
import java.util.List;


public class RapidShareMirrorConfig {
    private List<MirrorBean> ar = Collections.emptyList();
    private MirrorBean chosen;

    public List<MirrorBean> getAr() {
        return ar;
    }

    public void setAr(List<MirrorBean> ar) {
        this.ar = ar;
    }

    public MirrorBean getChosen() {
        return chosen;
    }

    public void setChosen(MirrorBean chosen) {
        this.chosen = chosen;
    }
}


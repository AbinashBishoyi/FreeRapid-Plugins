package cz.vity.freerapid.plugins.services.koukni;

/**
 * @author birchie
 */
public class KoukniSettingsConfig {
    private int qualitySetting;

    public void setVideoQuality(int qualitySetting) {
        this.qualitySetting = qualitySetting;
    }

    public int getVideoQuality() {
        return qualitySetting;
    }
}

package cz.vity.freerapid.plugins.services.youporn;

/**
 * @author birchie
 */
public class YouPornSettingsConfig {
    private int qualitySetting;

    public void setVideoQuality(int qualitySetting) {
        this.qualitySetting = qualitySetting;
    }

    public int getVideoQuality() {
        return qualitySetting;
    }
}

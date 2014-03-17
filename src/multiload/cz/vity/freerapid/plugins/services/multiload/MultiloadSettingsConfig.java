package cz.vity.freerapid.plugins.services.multiload;

/**
 *
 * @author JPEXS
 */
public class MultiloadSettingsConfig {
    private int serverSetting;

    public void setServerSetting(int qualitySetting) {
        this.serverSetting = qualitySetting;
    }

    public int getServerSetting() {
        return serverSetting;
    }

}

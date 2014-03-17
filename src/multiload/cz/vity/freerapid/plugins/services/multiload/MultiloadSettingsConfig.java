package cz.vity.freerapid.plugins.services.multiload;

/**
 *
 * @author JPEXS
 */
public class MultiloadSettingsConfig {
    private int serverSetting;

    public void setServerSetting(int serverIndex) {
        this.serverSetting = serverIndex;
    }

    public int getServerSetting() {
        return serverSetting;
    }

}

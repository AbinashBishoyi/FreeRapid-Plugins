package cz.vity.freerapid.plugins.services.nova;

/**
 * @author JPEXS
 */
public class NovaSettingsConfig {
    private int qualitySetting;

    /**
     * This maps qualityIndex to resolution
     */

    public void setQualitySetting(int qualitySetting) {
        this.qualitySetting = qualitySetting;
    }

    public int getQualitySetting() {
        return qualitySetting;
    }

}
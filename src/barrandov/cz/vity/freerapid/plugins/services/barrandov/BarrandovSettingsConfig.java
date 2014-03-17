package cz.vity.freerapid.plugins.services.barrandov;

/**
 * @author Kajda
 */
public class BarrandovSettingsConfig {
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
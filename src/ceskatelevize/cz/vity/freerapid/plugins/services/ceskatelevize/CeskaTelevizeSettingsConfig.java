package cz.vity.freerapid.plugins.services.ceskatelevize;

/**
 * @author JPEXS
 */
public class CeskaTelevizeSettingsConfig {
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
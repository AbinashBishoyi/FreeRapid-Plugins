package cz.vity.freerapid.plugins.services.iprima;

/**
 * @author JPEXS
 */
public class iPrimaSettingsConfig {
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
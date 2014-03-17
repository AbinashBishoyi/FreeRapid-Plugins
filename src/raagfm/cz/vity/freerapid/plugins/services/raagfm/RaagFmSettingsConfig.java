package cz.vity.freerapid.plugins.services.raagfm;

public class RaagFmSettingsConfig {
    private int qualitySetting;
    private final int[] bitrateMap = {128, 320};

    public int getQualitySetting() {
        return qualitySetting;
    }

    public void setQualitySetting(int qualitySetting) {
        this.qualitySetting = qualitySetting;
    }

    public int getQualityBitRate() {
        return bitrateMap[qualitySetting];
    }
}

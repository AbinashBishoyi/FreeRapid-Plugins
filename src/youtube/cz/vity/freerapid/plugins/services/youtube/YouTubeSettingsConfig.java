package cz.vity.freerapid.plugins.services.youtube;

/**
 * @author Kajda
 */
public class YouTubeSettingsConfig {
    private int qualitySetting;
    private boolean reversePlaylistOrder;

    public void setQualitySetting(int qualitySetting) {
        this.qualitySetting = qualitySetting;
    }

    public int getQualitySetting() {
        return qualitySetting;
    }

    public void setReversePlaylistOrder(boolean reversePlaylistOrder) {
        this.reversePlaylistOrder = reversePlaylistOrder;
    }

    public boolean isReversePlaylistOrder() {
        return reversePlaylistOrder;
    }
}
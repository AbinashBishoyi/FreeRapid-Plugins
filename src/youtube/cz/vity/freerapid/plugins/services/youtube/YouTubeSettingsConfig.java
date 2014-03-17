package cz.vity.freerapid.plugins.services.youtube;

/**
 * @author Kajda
 */
public class YouTubeSettingsConfig {
    private int qualitySetting;
    private boolean reversePlaylistOrder;
    private boolean downloadSubtitles;
    private int container;

    public static final int MIN_WIDTH = -2;
    public static final int MAX_WIDTH = -1;

    /**
     * This maps qualityIndex to resolution
     */
    private final int[] widthMap = {MIN_WIDTH, 240, 360, 480, MAX_WIDTH, 720, 1080}; //Due to quality settings in older versions, 4 is Highest quality available
    private final String[] containerExtensionMap = {"Any", ".mp4", ".flv", ".webm", ".3gp"};

    public void setQualitySetting(int qualitySetting) {
        this.qualitySetting = qualitySetting;
    }

    public int getQualitySetting() {
        return qualitySetting;
    }

    public int getQualityWidth() {
        return widthMap[qualitySetting];
    }

    public void setReversePlaylistOrder(boolean reversePlaylistOrder) {
        this.reversePlaylistOrder = reversePlaylistOrder;
    }

    public boolean isReversePlaylistOrder() {
        return reversePlaylistOrder;
    }

    public void setDownloadSubtitles(boolean downloadSubtitles) {
        this.downloadSubtitles = downloadSubtitles;
    }

    public boolean isDownloadSubtitles() {
        return downloadSubtitles;
    }

    public int getContainer() {
        return container;
    }

    public void setContainer(int container) {
        this.container = container;
    }

    public String getContainerExtension() {
        return containerExtensionMap[container];
    }
}
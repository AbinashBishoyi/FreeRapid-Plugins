package cz.vity.freerapid.plugins.services.youtube;

/**
 * @author Kajda
 */
public class YouTubeSettingsConfig {
    private int qualitySetting;
    private boolean reversePlaylistOrder;
    private boolean downloadSubtitles;
    private int container;
    private boolean convertToAudio;
    private AudioQuality audioQuality;

    public static final int MIN_WIDTH = -2;
    public static final int MAX_WIDTH = -1;
    public static final int ANY_CONTAINER = 0;

    /**
     * This maps qualityIndex to resolution
     */
    private final int[] videoResolutionMap = {MIN_WIDTH, 240, 360, 480, MAX_WIDTH, 720, 1080}; //Due to quality settings in older versions, 4 is Highest quality available
    private final String[] containerExtensionMap = {"Any", ".mp4", ".flv", ".webm", ".3gp"};

    public void setQualitySetting(int qualitySetting) {
        this.qualitySetting = qualitySetting;
    }

    public int getQualitySetting() {
        return qualitySetting;
    }

    public int getVideoResolution() {
        return videoResolutionMap[qualitySetting];
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

    public boolean isConvertToAudio() {
        return convertToAudio;
    }

    public void setConvertToAudio(boolean convertToAudio) {
        this.convertToAudio = convertToAudio;
    }

    public AudioQuality getAudioQuality() {
        return audioQuality;
    }

    public void setAudioQuality(AudioQuality audioQuality) {
        this.audioQuality = audioQuality;
    }
}
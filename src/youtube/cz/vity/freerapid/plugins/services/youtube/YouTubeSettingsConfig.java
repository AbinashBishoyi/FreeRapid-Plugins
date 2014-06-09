package cz.vity.freerapid.plugins.services.youtube;

/**
 * @author Kajda
 * @author ntoskrnl
 * @author tong2shot
 */
public class YouTubeSettingsConfig {
    private DownloadMode downloadMode = DownloadMode.downloadVideo;
    private VideoQuality videoQuality = VideoQuality._480;
    private Container container = Container.mp4;
    private AudioQuality convertAudioQuality = AudioQuality._192;
    private AudioQuality extractAudioQuality = AudioQuality._192;
    private boolean reversePlaylistOrder = false;
    private boolean downloadSubtitles = false;
    private boolean enableDash = false;
    private boolean enableInternalMultiplexer = true;

    public void setVideoQuality(VideoQuality videoQuality) {
        this.videoQuality = videoQuality;
    }

    public VideoQuality getVideoQuality() {
        return this.videoQuality;
    }

    public Container getContainer() {
        return container;
    }

    public void setContainer(Container container) {
        this.container = container;
    }

    public DownloadMode getDownloadMode() {
        return downloadMode;
    }

    public void setDownloadMode(DownloadMode downloadMode) {
        this.downloadMode = downloadMode;
    }

    public AudioQuality getConvertAudioQuality() {
        return convertAudioQuality;
    }

    public void setConvertAudioQuality(AudioQuality convertAudioQuality) {
        this.convertAudioQuality = convertAudioQuality;
    }

    public AudioQuality getExtractAudioQuality() {
        return extractAudioQuality;
    }

    public void setExtractAudioQuality(AudioQuality extractAudioQuality) {
        this.extractAudioQuality = extractAudioQuality;
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

    public boolean isEnableDash() {
        return enableDash;
    }

    public void setEnableDash(boolean enableDash) {
        this.enableDash = enableDash;
    }

    public boolean isEnableInternalMultiplexer() {
        return enableInternalMultiplexer;
    }

    public void setEnableInternalMultiplexer(boolean enableInternalMultiplexer) {
        this.enableInternalMultiplexer = enableInternalMultiplexer;
    }

    @Override
    public String toString() {
        return "YouTubeSettingsConfig{" +
                "downloadMode=" + downloadMode + (downloadMode == DownloadMode.convertToAudio ?
                ", convertAudioQuality=" + convertAudioQuality : (downloadMode == DownloadMode.extractAudio ?
                ", extractAudioQuality=" + extractAudioQuality :
                ", videoQuality=" + videoQuality +
                        ", container='" + container + '\'' +
                        ", enableDash=" + enableDash)) +
                '}';
    }
}

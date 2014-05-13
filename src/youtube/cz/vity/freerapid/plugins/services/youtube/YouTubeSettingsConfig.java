package cz.vity.freerapid.plugins.services.youtube;

/**
 * @author Kajda
 * @author ntoskrnl
 * @author tong2shot
 */
public class YouTubeSettingsConfig {
    private VideoQuality videoQuality = VideoQuality._480;
    private Container container = Container.mp4;
    private boolean convertToAudio = false;
    private AudioQuality audioQuality = AudioQuality._192;
    private boolean reversePlaylistOrder = false;
    private boolean downloadSubtitles = false;
    private boolean enableDash = false;

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

    @Override
    public String toString() {
        return "YouTubeSettingsConfig{" +
                "convertToAudio=" + convertToAudio + (convertToAudio ?
                ", audioQuality=" + audioQuality :
                ", videoQuality=" + videoQuality +
                        ", container='" + container + '\'' +
                        ", enableDash=" + enableDash) +
                '}';
    }
}

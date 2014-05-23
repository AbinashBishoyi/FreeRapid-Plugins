package cz.vity.freerapid.plugins.services.bbc;

/**
 * @author tong2shot
 */
public class SettingsConfig {

    private VideoQuality videoQuality = VideoQuality.Highest;
    private boolean downloadSubtitles = false;
    private RtmpPort rtmpPort = RtmpPort._1935;

    public VideoQuality getVideoQuality() {
        return videoQuality;
    }

    public void setVideoQuality(VideoQuality videoQuality) {
        this.videoQuality = videoQuality;
    }

    public boolean isDownloadSubtitles() {
        return downloadSubtitles;
    }

    public void setDownloadSubtitles(boolean downloadSubtitles) {
        this.downloadSubtitles = downloadSubtitles;
    }

    public RtmpPort getRtmpPort() {
        return rtmpPort;
    }

    public void setRtmpPort(RtmpPort rtmpPort) {
        this.rtmpPort = rtmpPort;
    }

    @Override
    public String toString() {
        return "SettingsConfig{" +
                "videoQuality=" + videoQuality +
                '}';
    }
}

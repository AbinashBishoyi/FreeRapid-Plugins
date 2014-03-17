package cz.vity.freerapid.plugins.services.videocopilotnet;

/**
 * @author tong2shot
 */
public class VideoCoPilotNetSettingsConfig {
    private VideoQuality videoQuality;
    private boolean downloadProject;

    public VideoQuality getVideoQuality() {
        return videoQuality;
    }

    public void setVideoQuality(final VideoQuality videoQuality) {
        this.videoQuality = videoQuality;
    }

    public boolean isDownloadProject() {
        return downloadProject;
    }

    public void setDownloadProject(boolean downloadProject) {
        this.downloadProject = downloadProject;
    }
}

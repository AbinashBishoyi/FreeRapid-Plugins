package cz.vity.freerapid.plugins.services.multiupload;

/**
 * @author ntoskrnl
 */
public class MultiUploadSettingsConfig {
    private boolean checkDownloadService;
    private boolean queueAllLinks;

    public MultiUploadSettingsConfig() {
        checkDownloadService = true;
        queueAllLinks = true;
    }

    public void setCheckDownloadService(final boolean checkDownloadService) {
        this.checkDownloadService = checkDownloadService;
    }

    public boolean getCheckDownloadService() {
        return this.checkDownloadService;
    }

    public boolean isQueueAllLinks() {
        return queueAllLinks;
    }

    public void setQueueAllLinks(boolean queueAllLinks) {
        this.queueAllLinks = queueAllLinks;
    }
}
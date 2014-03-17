package cz.vity.freerapid.plugins.services.multiupload;

/**
 * @author ntoskrnl
 */
public class MultiUploadSettingsConfig {
    private boolean checkDownloadService;

    public MultiUploadSettingsConfig() {
        setCheckDownloadService(true);
    }

    public void setCheckDownloadService(final boolean checkDownloadService) {
        this.checkDownloadService = checkDownloadService;
    }

    public boolean getCheckDownloadService() {
        return this.checkDownloadService;
    }
}
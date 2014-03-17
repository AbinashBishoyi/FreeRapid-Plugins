package cz.vity.freerapid.plugins.services.multiupload;

/**
 * @author ntoskrnl
 */
public class MultiUploadSettingsConfig {
    private boolean checkDownloadService;
    private String[] services;

    public void setDefault() {
        setCheckDownloadService(true);
        setServices(new String[]{"RapidShare.com", "MegaUpload.com", "HotFile.com", "DepositFiles.com", "zShare.net", "Badongo.com", "Uploading.com", "2shared.com"});
    }

    public void setCheckDownloadService(final boolean checkDownloadService) {
        this.checkDownloadService = checkDownloadService;
    }

    public boolean getCheckDownloadService() {
        return this.checkDownloadService;
    }

    public void setServices(final String[] services) {
        this.services = services;
    }

    public String[] getServices() {
        return this.services;
    }
}
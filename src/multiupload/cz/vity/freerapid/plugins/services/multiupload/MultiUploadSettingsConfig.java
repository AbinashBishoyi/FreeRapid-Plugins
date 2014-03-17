package cz.vity.freerapid.plugins.services.multiupload;

/**
 * @author ntoskrnl
 */
public class MultiUploadSettingsConfig {
    private String[] services;

    public void setServices() {
        setServices(new String[]{"RapidShare.com", "MegaUpload.com", "HotFile.com", "DepositFiles.com", "zShare.net", "Badongo.com", "Uploading.com", "2shared.com"});
    }

    public void setServices(final String[] services) {
        this.services = services;
    }

    public String[] getServices() {
        return this.services;
    }
}
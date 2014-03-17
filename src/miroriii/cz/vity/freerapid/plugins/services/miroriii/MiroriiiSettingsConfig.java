package cz.vity.freerapid.plugins.services.miroriii;

/**
 * @author ntoskrnl
 */
public class MiroriiiSettingsConfig {
    private boolean checkDownloadService;
    private boolean AddLink;
    private String[] services;

    public void setDefault() {
        setCheckDownloadService(true);
        setAddLink(true);
        setServices(new String[]{"MegaUpload.com", "RapidShare.com", "HotFile.com", "Uploaded.to", "Free.fr", "Sharehoster"});
    }

    public void setCheckDownloadService(final boolean checkDownloadService) {
        this.checkDownloadService = checkDownloadService;
    }

    public void setAddLink(final boolean AddLink) {
        this.AddLink = AddLink;
    }

    public boolean getCheckDownloadService() {
        return this.checkDownloadService;
    }

    public boolean getAddLink() {
        return this.AddLink;
    }

    public void setServices(final String[] services) {
        this.services = services;
    }

    public String[] getServices() {
        return this.services;
    }
}
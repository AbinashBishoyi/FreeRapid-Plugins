package cz.vity.freerapid.plugins.services.miroriii;

/**
 * @author ntoskrnl
 */
public class MiroriiiSettingsConfig {
    private boolean checkDownloadService;
    private boolean addLink;
    private String[] services;

    public void setDefault() {
        setCheckDownloadService(true);
        setAddLink(true);
        setServices(new String[]{"MegaUpload.com", "RapidShare.com", "HotFile.com", "Uploaded.to", "Free.fr", "Sharehoster"});
    }

    public void setCheckDownloadService(final boolean checkDownloadService) {
        this.checkDownloadService = checkDownloadService;
    }

    public void setAddLink(final boolean addLink) {
        this.addLink = addLink;
    }

    public boolean getCheckDownloadService() {
        return this.checkDownloadService;
    }

    public boolean getAddLink() {
        return this.addLink;
    }

    public void setServices(final String[] services) {
        this.services = services;
    }

    public String[] getServices() {
        return this.services;
    }
}
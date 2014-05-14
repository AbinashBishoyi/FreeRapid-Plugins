package cz.vity.freerapid.plugins.services.bookdl;

/**
 * @author CrazyCoder
 */
public class BookDLSettingsConfig {
    private boolean downloadPDF = true;
    private boolean downloadEPUB = true;
    private boolean downloadMOBI = true;

    public boolean isDownloadPDF() {
        return downloadPDF;
    }

    public void setDownloadPDF(boolean downloadPDF) {
        this.downloadPDF = downloadPDF;
    }

    public boolean isDownloadEPUB() {
        return downloadEPUB;
    }

    public void setDownloadEPUB(boolean downloadEPUB) {
        this.downloadEPUB = downloadEPUB;
    }

    public boolean isDownloadMOBI() {
        return downloadMOBI;
    }

    public void setDownloadMOBI(boolean downloadMOBI) {
        this.downloadMOBI = downloadMOBI;
    }
}

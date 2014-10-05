package cz.vity.freerapid.plugins.services.youtube;

/**
 * @author tong2shot
 */
enum DownloadMode {
    downloadVideo,
    convertToAudio,
    extractAudio;

    @Override
    public String toString() {
        switch (this) {
            case convertToAudio:
                return "Convert to Audio (MP3)";
            case extractAudio:
                return "Extract Audio";
            default:
                return "Download Video";
        }
    }
}

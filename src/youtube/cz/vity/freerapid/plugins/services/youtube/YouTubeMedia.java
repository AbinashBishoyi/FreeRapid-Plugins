package cz.vity.freerapid.plugins.services.youtube;

public class YouTubeMedia implements Comparable<YouTubeMedia> {
    private int itagCode;
    private String container;
    private String fileExtension;
    private int videoResolution;
    private String audioEncoding;
    private int audioBitrate;

    public YouTubeMedia(int itagCode, String container, String fileExtension, int videoResolution, String audioEncoding, int audioBitrate) {
        this.itagCode = itagCode;
        this.container = container;
        this.fileExtension = fileExtension;
        this.videoResolution = videoResolution;
        this.audioEncoding = audioEncoding;
        this.audioBitrate = audioBitrate;
    }

    public int getItagCode() {
        return itagCode;
    }

    public String getContainer() {
        return container;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public int getVideoResolution() {
        return videoResolution;
    }

    public String getAudioEncoding() {
        return audioEncoding;
    }

    public int getAudioBitrate() {
        return audioBitrate;
    }

    @Override
    public int compareTo(YouTubeMedia that) {
        return Integer.valueOf(this.videoResolution).compareTo(that.videoResolution);
    }

    @Override
    public String toString() {
        return "YouTubeMedia{" +
                "itagCode=" + itagCode +
                ", container='" + container + '\'' +
                ", videoResolution=" + videoResolution +
                ", audioEncoding='" + audioEncoding + '\'' +
                ", audioBitrate=" + audioBitrate +
                '}';
    }
}

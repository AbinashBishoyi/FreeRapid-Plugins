package cz.vity.freerapid.plugins.services.youtube;

public class YouTubeMedia {
    private int itagCode;
    private Container container;
    private int videoQuality; // deliberately not using VideoQuality, reason : flexibility, it's possible that YT introduces video quality which is not listed in VideoQuality data structure
    private String audioEncoding;
    private int audioBitrate;

    public YouTubeMedia(int itagCode, int videoQuality) {
        this.itagCode = itagCode;
        this.container = getContainer(itagCode);
        this.videoQuality = videoQuality;
        this.audioEncoding = getAudioEncoding(itagCode);
        this.audioBitrate = getAudioBitrate(itagCode);
    }

    //source : http://en.wikipedia.org/wiki/YouTube#Quality_and_codecs
    public static Container getContainer(int itagCode) {
        switch (itagCode) {
            case 13:
            case 17:
            case 36:
                return Container._3gp;
            case 18:
            case 22:
            case 37:
            case 38:
            case 82:
            case 83:
            case 84:
            case 85:
                return Container.mp4;
            case 43:
            case 44:
            case 45:
            case 46:
            case 100:
            case 101:
            case 102:
                return Container.webm;
            default:
                return Container.flv;
        }
    }

    public static String getAudioEncoding(int itagCode) {
        switch (itagCode) {
            case 5:
            case 6:
                return "MP3";
            case 43:
            case 44:
            case 45:
            case 46:
                return "Vorbis";
            default:
                return "AAC";
        }
    }

    public static int getAudioBitrate(int itagCode) {
        switch (itagCode) {
            case 17:
                return 24;
            case 36:
                return 38;
            case 5:
            case 6:
                return 64;
            case 18:
            case 82:
            case 83:
                return 96;
            case 34:
            case 35:
            case 43:
            case 44:
                return 128;
            case 84:
            case 85:
                return 152;
            default:
                return 192;
        }
    }

    public int getItagCode() {
        return itagCode;
    }

    public Container getContainer() {
        return container;
    }

    public int getVideoQuality() {
        return videoQuality;
    }

    public String getAudioEncoding() {
        return audioEncoding;
    }

    public int getAudioBitrate() {
        return audioBitrate;
    }

    @Override
    public String toString() {
        return "YouTubeMedia{" +
                "itagCode=" + itagCode +
                ", container='" + container + '\'' +
                ", videoQuality=" + videoQuality + 'p' +
                ", audioEncoding='" + audioEncoding + '\'' +
                ", audioBitrate=" + audioBitrate +
                '}';
    }
}

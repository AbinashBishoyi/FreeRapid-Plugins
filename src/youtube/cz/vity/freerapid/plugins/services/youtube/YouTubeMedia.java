package cz.vity.freerapid.plugins.services.youtube;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;

public class YouTubeMedia {
    private int itagCode;
    private Container container;
    private int videoQuality; // deliberately not using VideoQuality, reason : flexibility, it's possible that YT introduces video quality which is not listed in VideoQuality data structure
    private String audioEncoding;
    private int audioBitrate;
    private String url;
    private String signature;
    private boolean cipherSignature;

    public YouTubeMedia(int itagCode, String url, String signature, boolean cipherSignature) throws ErrorDuringDownloadingException {
        this.itagCode = itagCode;
        this.container = getContainer(itagCode);
        this.videoQuality = getVideoResolution(itagCode);
        this.audioEncoding = getAudioEncoding(itagCode);
        this.audioBitrate = getAudioBitrate(itagCode);
        this.url = url;
        this.signature = signature;
        this.cipherSignature = cipherSignature;
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

    public static int getVideoResolution(int itagCode) throws ErrorDuringDownloadingException {
        switch (itagCode) {
            case 17:
            case 160:
                return 144;
            case 5:
            case 36:
            case 83:
            case 133:
                return 240;
            case 6:
                return 270;
            case 18:
            case 34:
            case 43:
            case 82:
            case 100:
            case 101:
            case 134:
                return 360;
            case 35:
            case 44:
            case 135:
                return 480;
            case 85:
                return 520;
            case 22:
            case 45:
            case 84:
            case 102:
            case 120:
            case 136:
                return 720;
            case 37:
            case 46:
            case 137:
                return 1080;
            case 38:
                return 3072;
            default:
                throw new PluginImplementationException("Unknown video resolution for itagCode=" + itagCode);
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

    public String getUrl() {
        return url;
    }

    public String getSignature() {
        return signature;
    }

    public boolean isCipherSignature() {
        return cipherSignature;
    }

    @Override
    public String toString() {
        return "YouTubeMedia{" +
                "itagCode=" + itagCode +
                ", container=" + container +
                ", videoQuality=" + videoQuality +
                ", audioEncoding='" + audioEncoding + '\'' +
                ", audioBitrate=" + audioBitrate +
                //", url='" + url + '\'' +
                //", signature='" + signature + '\'' +
                //", cipherSignature=" + cipherSignature +
                '}';
    }
}

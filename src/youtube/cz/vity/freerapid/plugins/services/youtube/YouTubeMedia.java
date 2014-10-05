package cz.vity.freerapid.plugins.services.youtube;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;

class YouTubeMedia {
    private final int itag;
    private final Container container;
    private final int videoQuality; // deliberately not using VideoQuality, reason : flexibility, it's possible that YT introduces video quality which is not listed in VideoQuality data structure
    private final String audioEncoding;
    private final int audioBitrate;
    private final String url;
    private final String signature;
    private final boolean cipherSignature;

    public YouTubeMedia(int itag, String url, String signature, boolean cipherSignature) throws ErrorDuringDownloadingException {
        this.itag = itag;
        this.container = getContainer(itag);
        this.videoQuality = (container == Container.dash_a ? -1 : getVideoResolution(itag));
        this.audioEncoding = (isDashVideo() ? "None" : getAudioEncoding(itag));
        this.audioBitrate = (isDashVideo() ? -1 : getAudioBitrate(itag));
        this.url = url;
        this.signature = signature;
        this.cipherSignature = cipherSignature;
    }

    //source : http://en.wikipedia.org/wiki/YouTube#Quality_and_codecs
    private Container getContainer(int itag) {
        switch (itag) {
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
            case 133:
            case 134:
            case 135:
            case 136:
            case 137:
            case 138:
            case 160:
            case 264:
                return Container.dash_v;
            case 242:
            case 243:
            case 244:
            case 247:
            case 248:
                return Container.dash_v_vpx;
            case 139:
            case 140:
            case 141:
            case 171:
            case 172:
                return Container.dash_a;
            default:
                return Container.flv;
        }
    }

    private String getAudioEncoding(int itag) {
        switch (itag) {
            case 5:
            case 6:
                return "MP3";
            case 43:
            case 44:
            case 45:
            case 46:
            case 171:
            case 172:
                return "Vorbis";
            default:
                return "AAC";
        }
    }

    private int getAudioBitrate(int itag) {
        switch (itag) {
            case 17:
                return 24;
            case 36:
                return 38;
            case 139:
                return 48;
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
            case 140:
            case 171:
                return 128;
            case 84:
            case 85:
                return 152;
            case 141:
                return 256;
            default:
                return 192;
        }
    }

    private int getVideoResolution(int itag) throws ErrorDuringDownloadingException {
        switch (itag) {
            case 17:
            case 160:
                return 144;
            case 5:
            case 36:
            case 83:
            case 133:
            case 242:
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
            case 243:
                return 360;
            case 35:
            case 44:
            case 135:
            case 244:
                return 480;
            case 85:
                return 520;
            case 22:
            case 45:
            case 84:
            case 102:
            case 120:
            case 136:
            case 247:
                return 720;
            case 37:
            case 46:
            case 137:
            case 248:
                return 1080;
            case 264:
                return 1440;
            case 38:
            case 138: //138=original
                return 3072;
            default:
                throw new PluginImplementationException("Unknown video resolution for itag=" + itag);
        }
    }

    public boolean isVid2AudSupported() {
        return ((container == Container.mp4 || container == Container.flv || container == Container.dash_a)
                && (audioEncoding.equalsIgnoreCase("MP3") || audioEncoding.equalsIgnoreCase("AAC")));
    }

    public boolean isAudioExtractSupported() {
        return isVid2AudSupported();
    }

    public boolean isDashVideo() {
        return (container == Container.dash_v) || (container == Container.dash_v_vpx);
    }

    public boolean isDash() {
        return isDashVideo() || (container == Container.dash_a);
    }

    public int getItag() {
        return itag;
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
                "itag=" + itag +
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

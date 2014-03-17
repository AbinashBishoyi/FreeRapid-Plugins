package cz.vity.freerapid.plugins.services.ceskatelevize;

/**
 * @author JPEXS
 * @author tong2shot
 */
public class Video implements Comparable<Video> {
    private final String src;
    private final int videoQuality; // deliberately not using VideoQuality, reason : flexibility, to anticipate video quality which is not listed in VideoQuality data structure

    public Video(String src, int videoQuality) {
        this.src = src;
        this.videoQuality = videoQuality;
    }

    public String getSrc() {
        return src;
    }

    public int getVideoQuality() {
        return videoQuality;
    }

    @Override
    public String toString() {
        return "Video{" +
                "src='" + src + '\'' +
                ", videoQuality=" + videoQuality + 'p' +
                '}';
    }

    @Override
    public int compareTo(Video that) {
        return Integer.valueOf(this.videoQuality).compareTo(that.videoQuality);
    }
}

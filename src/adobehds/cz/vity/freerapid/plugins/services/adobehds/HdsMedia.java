package cz.vity.freerapid.plugins.services.adobehds;

/**
 * @author ntoskrnl
 */
public class HdsMedia implements Comparable<HdsMedia> {
    private final String url;
    private final int bitrate;
    private final int fragmentCount;

    public HdsMedia(final String url, final int bitrate, final int fragmentCount) {
        this.url = url;
        this.bitrate = bitrate;
        this.fragmentCount = fragmentCount;
    }

    public String getUrl() {
        return url;
    }

    public int getFragmentCount() {
        return fragmentCount;
    }

    public int getBitrate() {
        return bitrate;
    }

    @Override
    public int compareTo(final HdsMedia that) {
        return Integer.valueOf(this.bitrate).compareTo(that.bitrate);
    }

    @Override
    public String toString() {
        return "Media{" +
                "url='" + url + '\'' +
                ", bitrate=" + bitrate +
                ", fragmentCount=" + fragmentCount +
                '}';
    }
}

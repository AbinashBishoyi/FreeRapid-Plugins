package cz.vity.freerapid.plugins.services.adobehds;

/**
 * @author ntoskrnl
 */
public class HdsMedia implements Comparable<HdsMedia> {
    private final String url;
    private final String urlQuery;
    private final int bitrate;
    private final int fragmentCount;

    public HdsMedia(final String url, String urlQuery, final int bitrate, final int fragmentCount) {
        this.url = url;
        this.urlQuery = urlQuery;
        this.bitrate = bitrate;
        this.fragmentCount = fragmentCount;
    }

    public String getUrl() {
        return url;
    }

    public String getUrlQuery() {
        return urlQuery;
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

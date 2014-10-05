package cz.vity.freerapid.plugins.services.dailymotion;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author tong2shot
 */
public enum VideoQuality {
    _240(240, "ld"), //"stream_h264_ld_url"
    _380(380, "sd", ""), //"stream_h264_url"
    _480(480, "hq"),
    _720(720, "hd720", "hd"),
    _1080(1080, "hd1080");

    private final int quality;
    private final String name;
    private final String qualityToken1;
    private final String qualityToken2;

    private VideoQuality(int quality, String qualityToken1) {
        this.quality = quality;
        this.name = quality + "p";
        this.qualityToken1 = qualityToken1;
        this.qualityToken2 = qualityToken1;
    }

    private VideoQuality(int quality, String qualityToken1, String qualityToken2) {
        this.quality = quality;
        this.name = quality + "p";
        this.qualityToken1 = qualityToken1;
        this.qualityToken2 = qualityToken2;
    }

    public int getQuality() {
        return quality;
    }

    public String getName() {
        return name;
    }

    public String getQualityToken1() {
        return qualityToken1;
    }

    public String getQualityToken2() {
        return qualityToken2;
    }

    @Override
    public String toString() {
        return name;
    }

    public static VideoQuality[] getItems() {
        final VideoQuality[] items = values();
        Arrays.sort(items, Collections.reverseOrder());
        return items;
    }
}

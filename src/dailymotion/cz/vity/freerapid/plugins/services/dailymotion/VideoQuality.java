package cz.vity.freerapid.plugins.services.dailymotion;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author tong2shot
 */
public enum VideoQuality {
    _240(240),
    _380(380),
    _480(480),
    _720(720),
    _1080(1080);

    private final int quality;
    private final String name;
    private final String qualityToken;

    private VideoQuality(int quality) {
        this.quality = quality;
        this.name = quality + "p";
        switch (quality) {
            case 240:
                this.qualityToken = "ldURL";
                break;
            case 380:
                this.qualityToken = "sdURL";
                break;
            case 480:
                this.qualityToken = "hqURL";
                break;
            case 720:
                this.qualityToken = "hd720URL";
                break;
            default:
                this.qualityToken = "hd1080URL";
                break;
        }
    }

    public int getQuality() {
        return quality;
    }

    public String getName() {
        return name;
    }

    public String getQualityToken() {
        return qualityToken;
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

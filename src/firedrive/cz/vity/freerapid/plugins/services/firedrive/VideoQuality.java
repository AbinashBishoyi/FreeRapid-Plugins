package cz.vity.freerapid.plugins.services.firedrive;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author tong2shot
 */
public enum VideoQuality {
    Mobile,
    Low,
    High;

    public static VideoQuality[] getItems() {
        final VideoQuality[] items = values();
        Arrays.sort(items, Collections.reverseOrder());
        return items;
    }
}

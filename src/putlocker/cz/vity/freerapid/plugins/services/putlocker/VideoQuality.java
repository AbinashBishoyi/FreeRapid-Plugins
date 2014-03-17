package cz.vity.freerapid.plugins.services.putlocker;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author ntoskrnl
 */
public enum VideoQuality {
    Low,
    High,
    Mobile;

    public static VideoQuality[] getItems() {
        final VideoQuality[] items = values();
        Arrays.sort(items, Collections.reverseOrder());
        return items;
    }
}

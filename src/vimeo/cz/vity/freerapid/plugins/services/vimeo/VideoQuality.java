package cz.vity.freerapid.plugins.services.vimeo;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author ntoskrnl
 */
public enum VideoQuality {
    Mobile,
    SD,
    HD;

    public static VideoQuality[] getItems() {
        final VideoQuality[] items = values();
        Arrays.sort(items, Collections.reverseOrder());
        return items;
    }
}

package cz.vity.freerapid.plugins.services.youtube;

import java.util.Locale;

/**
 * @author tong2shot
 */
public enum Container {
    Any,
    mp4,
    flv,
    webm,
    _3gp;

    private final String name;
    private final String fileExt;

    private Container() {
        switch (this) {
            case Any:
                name = "Any container";
                fileExt = "Any";
                break;
            case _3gp:
                name = "3GP";
                fileExt = ".3gp";
                break;
            default:
                name = name().toUpperCase(Locale.ENGLISH);
                fileExt = "." + name();
                break;
        }
    }

    public String getName() {
        return name;
    }

    public String getFileExt() {
        return fileExt;
    }

    @Override
    public String toString() {
        return name;
    }

    public static Container[] getItems() {
        return values();
    }
}

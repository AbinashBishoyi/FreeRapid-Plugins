package cz.vity.freerapid.plugins.services.youtube;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author tong2shot
 */
enum Container {
    Any(0),
    mp4(1),
    flv(2),
    webm(3),
    _3gp(4),
    dash_v(5), //dash video with H264 video encoding
    dash_a(6),
    dash_v_vpx(7); //dash video with vp9 (at this moment can't find vp8 variant) video encoding

    private final String name;
    private final String fileExt;

    private Container(int index) {
        switch (index) {
            case 0:
                name = "Any container";
                fileExt = "Any";
                break;
            case 4:
                name = "3GP";
                fileExt = ".3gp";
                break;
            case 5:
                name = "DASH Video";
                fileExt = ".m4v";
                break;
            case 6:
                name = "DASH Audio";
                fileExt = ".m4a";
                break;
            case 7:
                name = "DASH Video (VP9)";
                fileExt = ".m4v";
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
        List<Container> items = new ArrayList<Container>();
        for (Container item : values()) {
            if ((item == dash_v) || (item == dash_v_vpx) || (item == dash_a)) { //only drunk users choose DASH, hide it!
                continue;
            }
            items.add(item);
        }
        return items.toArray(new Container[items.size()]);
    }
}

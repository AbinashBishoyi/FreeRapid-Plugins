package cz.vity.freerapid.plugins.services.youtube.srt;

import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import jlibs.xml.sax.binding.Attr;
import jlibs.xml.sax.binding.Binding;
import jlibs.xml.sax.binding.Relation;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Vity
 */
@SuppressWarnings("unused")
@Binding("transcript")
public class TranscriptionBinding {

    @Binding.Start
    public static List<SrtItem> onStart() {
        return new LinkedList<SrtItem>();
    }

    @Binding.Text("text")
    public static String onText(String text) {
        return text;
    }

    @Binding.Start("text")
    public static void onText(List<SrtItem> list, @Attr String start, @Attr String dur) {
        if (start == null) {
            throw new IllegalArgumentException("'start' attribute cannot be null");
        }
        if (dur == null) {
            throw new IllegalArgumentException("'dur' attribute cannot be null");
        }
        SrtItem item = new SrtItem(list.size() + 1, start, dur);
        list.add(item);
    }

    @Relation.Finish("text")
    public static void relateText(List<SrtItem> list, String text) {
        list.get(list.size() - 1).setText(PlugUtils.unescapeHtml(text));
    }

}



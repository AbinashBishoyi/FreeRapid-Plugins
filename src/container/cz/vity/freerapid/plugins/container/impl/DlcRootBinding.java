package cz.vity.freerapid.plugins.container.impl;

import cz.vity.freerapid.plugins.container.FileInfo;
import jlibs.xml.sax.binding.Binding;
import jlibs.xml.sax.binding.Relation;

import java.util.LinkedList;
import java.util.List;

/**
 * TODO handle comments
 *
 * @author ntoskrnl
 */
@SuppressWarnings("UnusedDeclaration")
@Binding("dlc")
class DlcRootBinding {

    @Binding.Start
    public static List<FileInfo> onStart() {
        return new LinkedList<FileInfo>();
    }

    @Binding.Element(element = "content/package/file", clazz = DlcFileBinding.class)
    public static void onFile() {
    }

    @Relation.Finish("content/package/file")
    public static void relateFile(List<FileInfo> infoList, FileInfo info) {
        if (info != null) {
            infoList.add(info);
        }
    }

}
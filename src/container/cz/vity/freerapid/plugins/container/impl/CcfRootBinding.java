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
@Binding("CryptLoad")
class CcfRootBinding {

    @Binding.Start
    public static List<FileInfo> onStart() {
        return new LinkedList<FileInfo>();
    }

    @Binding.Element(element = "Package/Download", clazz = CcfDownloadBinding.class)
    public static void onDownload() {
    }

    @Relation.Finish("Package/Download")
    public static void relateDownload(List<FileInfo> infoList, FileInfo info) {
        if (info != null) {
            infoList.add(info);
        }
    }

}

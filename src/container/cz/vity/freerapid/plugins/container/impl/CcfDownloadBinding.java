package cz.vity.freerapid.plugins.container.impl;

import cz.vity.freerapid.plugins.container.FileInfo;
import cz.vity.freerapid.utilities.LogUtils;
import jlibs.xml.sax.binding.Attr;
import jlibs.xml.sax.binding.Binding;
import jlibs.xml.sax.binding.Relation;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

/**
 * @author ntoskrnl
 */
@SuppressWarnings("UnusedDeclaration")
@Binding("Download")
class CcfDownloadBinding {
    private final static Logger logger = Logger.getLogger(CcfDownloadBinding.class.getName());

    @Binding.Start
    public static FileInfo onStart(@Attr("Url") String url) {
        try {
            return new FileInfo(new URL(url));
        } catch (MalformedURLException e) {
            LogUtils.processException(logger, e);
            return null;
        }
    }

    @Binding.Text({"FileName", "FileSize"})
    public static String onText(String text) {
        return text;
    }

    @Relation.Finish("FileName")
    public static void relateFileName(FileInfo info, String fileName) {
        if (info != null) {
            info.setFileName(fileName);
        }
    }

    @Relation.Finish("FileSize")
    public static void relateFileSize(FileInfo info, String fileSize) {
        if (info != null) {
            try {
                info.setFileSize(Long.parseLong(fileSize));
            } catch (NumberFormatException e) {
                LogUtils.processException(logger, e);
            }
        }
    }

}

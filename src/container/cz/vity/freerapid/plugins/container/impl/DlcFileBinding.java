package cz.vity.freerapid.plugins.container.impl;

import cz.vity.freerapid.plugins.container.FileInfo;
import cz.vity.freerapid.utilities.LogUtils;
import jlibs.xml.sax.binding.Binding;
import jlibs.xml.sax.binding.Relation;
import jlibs.xml.sax.binding.Temp;
import org.apache.commons.codec.binary.Base64;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

/**
 * @author ntoskrnl
 */
@SuppressWarnings("UnusedDeclaration")
@Binding("file")
class DlcFileBinding {
    private final static Logger logger = Logger.getLogger(CcfDownloadBinding.class.getName());

    @Binding.Text({"url", "filename", "size"})
    public static String onText(String text) {
        return text;
    }

    @Relation.Finish({"url", "filename", "size"})
    public static String relateText(String text) {
        if (text != null) {
            try {
                return new String(Base64.decodeBase64(text), "UTF-8");
            } catch (Exception e) {
                LogUtils.processException(logger, e);
            }
        }
        return null;
    }

    @Binding.Finish
    public static FileInfo onFinish(@Temp String url, @Temp String filename, @Temp String size) {
        if (url != null) {
            try {
                FileInfo info = new FileInfo(new URL(url));
                if (filename != null) {
                    info.setFileName(filename);
                }
                if (size != null) {
                    try {
                        info.setFileSize(Long.parseLong(size));
                    } catch (NumberFormatException e) {
                        LogUtils.processException(logger, e);
                    }
                }
                return info;
            } catch (MalformedURLException e) {
                LogUtils.processException(logger, e);
            }
        }
        return null;
    }

}

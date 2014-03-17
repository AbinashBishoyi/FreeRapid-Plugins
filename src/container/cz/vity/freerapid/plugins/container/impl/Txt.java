package cz.vity.freerapid.plugins.container.impl;

import cz.vity.freerapid.plugins.container.ContainerFormat;
import cz.vity.freerapid.plugins.container.ContainerPlugin;
import cz.vity.freerapid.plugins.container.ContainerUtils;
import cz.vity.freerapid.plugins.container.FileInfo;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import cz.vity.freerapid.utilities.Utils;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author ntoskrnl
 */
public class Txt implements ContainerFormat {
    private final static Logger logger = Logger.getLogger(Txt.class.getName());
    private final static String NEWLINE = Utils.getSystemLineSeparator();

    public static String[] getSupportedFiles() {
        return new String[]{"txt"};
    }

    public Txt(final ContainerPlugin plugin) {
    }

    @Override
    public List<FileInfo> read(final InputStream is) throws Exception {
        final List<FileInfo> list = new ArrayList<FileInfo>();
        final Matcher matcher = PlugUtils.matcher("(https?://\\S+)", ContainerUtils.readToString(is));
        while (matcher.find()) {
            try {
                list.add(new FileInfo(new URL(matcher.group(1))));
            } catch (MalformedURLException e) {
                LogUtils.processException(logger, e);
            }
        }
        return list;
    }

    @Override
    public void write(final List<FileInfo> files, final OutputStream os) throws Exception {
        final Writer w = new OutputStreamWriter(os, "UTF-8");
        for (final FileInfo file : files) {
            w.append(file.getFileUrl().toString());
            w.append(NEWLINE);
        }
        w.flush();
    }

}

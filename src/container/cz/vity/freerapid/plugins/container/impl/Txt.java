package cz.vity.freerapid.plugins.container.impl;

import cz.vity.freerapid.plugins.container.ContainerFormat;
import cz.vity.freerapid.plugins.container.ContainerPlugin;
import cz.vity.freerapid.plugins.container.FileInfo;
import cz.vity.freerapid.utilities.LogUtils;
import cz.vity.freerapid.utilities.Utils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

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
        final List<FileInfo> list = new LinkedList<FileInfo>();
        final Reader r = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        boolean lastWasWhitespace = true;
        int c;
        while ((c = r.read()) != -1) {
            if (lastWasWhitespace && c == 'h') {
                final char[] chars = peek(r, 7);
                if ((chars[0] == 't' && chars[1] == 't' && chars[2] == 'p')
                        && ((chars[3] == ':' && chars[4] == '/' && chars[5] == '/')
                        || (chars[3] == 's' && chars[4] == ':' && chars[5] == '/' && chars[6] == '/'))) {
                    final String url = 'h' + readUntilWhitespace(r);
                    try {
                        list.add(new FileInfo(new URL(url)));
                    } catch (final MalformedURLException e) {
                        LogUtils.processException(logger, e);
                    }
                    lastWasWhitespace = true;
                    continue;
                }
            }
            lastWasWhitespace = Character.isWhitespace(c);
        }
        return list;
    }

    private char[] peek(final Reader r, final int numChars) throws Exception {
        r.mark(numChars);
        final char[] chars = new char[numChars];
        r.read(chars);
        r.reset();
        return chars;
    }

    private String readUntilWhitespace(final Reader r) throws Exception {
        final StringBuilder sb = new StringBuilder();
        int c;
        while ((c = r.read()) != -1 && !Character.isWhitespace(c)) {
            sb.append((char) c);
        }
        return sb.toString();
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

package cz.vity.freerapid.plugins.services.itv;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * DetectEncoding.java - Returns the character encoding of an input stream containin an XML file.<br/>
 * Copyright (c) 2009 Alexander Hristov .
 * <p/>
 * Licensed under the LGPL License - http://www.gnu.org/licenses/lgpl.txt
 * <p/>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */


class DetectEncoding {

    /**
     * Returns the character encoding of an input stream containin an XML file.<br/>
     * <p/>
     * The encoding is detected using the guidelines specified in the
     * <a href='http://www.w3.org/TR/xml/#sec-guessing'>XML W3C Specification</a>,
     * and the method was designed to be as fast as possible, without extensive
     * string operations or regular expressions<br/> <br/>
     * <p/>
     * <code>
     * A sample use would be<br/><br/>
     * InputStream in = ...; <br/>
     * String encoding = detectEncoding(in);<br/>
     * BufferedReader reader = new BufferedReader(new InputStreamReader(in,encoding)); <br/>
     * </code><br/>
     * <p/>
     * and from that point you can happily read text from the input stream.
     *
     * @param in Stream containing the data to be read. The stream must support
     *           mark()/reset(), otherwise the caller should wrap that stream in a
     *           {@link java.io.BufferedInputStream} before invokin the method. After the
     *           call, the stream is positioned at the &lt; character (this means
     *           that if there were any byte-order-marks, they are skipped).
     * @return Detected encoding, using the canonical name in java.io (see <a
     * href=
     * 'http://java.sun.com/j2se/1.4.2/docs/guide/intl/encoding.doc.html'>Supported
     * Encodings</a> ).
     * @author Alexander Hristov
     */

    public static String detectEncoding(InputStream in) throws IOException {
        String encoding = null;
        in.mark(400);
        int ignoreBytes = 0;
        boolean readEncoding = false;
        byte[] buffer = new byte[400];
        int read = in.read(buffer, 0, 4);
        switch (buffer[0]) {
            case (byte) 0x00:
                if (buffer[1] == (byte) 0x00 && buffer[2] == (byte) 0xFE
                        && buffer[3] == (byte) 0xFF) {
                    ignoreBytes = 4;
                    encoding = "UTF_32BE";
                } else if (buffer[1] == (byte) 0x00 && buffer[2] == (byte) 0x00
                        && buffer[3] == (byte) 0x3C) {
                    encoding = "UTF_32BE";
                    readEncoding = true;
                } else if (buffer[1] == (byte) 0x3C && buffer[2] == (byte) 0x00
                        && buffer[3] == (byte) 0x3F) {
                    encoding = "UnicodeBigUnmarked";
                    readEncoding = true;
                }
                break;
            case (byte) 0xFF:
                if (buffer[1] == (byte) 0xFE && buffer[2] == (byte) 0x00
                        && buffer[3] == (byte) 0x00) {
                    ignoreBytes = 4;
                    encoding = "UTF_32LE";
                } else if (buffer[1] == (byte) 0xFE) {
                    ignoreBytes = 2;
                    encoding = "UnicodeLittleUnmarked";
                }
                break;

            case (byte) 0x3C:
                readEncoding = true;
                if (buffer[1] == (byte) 0x00 && buffer[2] == (byte) 0x00
                        && buffer[3] == (byte) 0x00) {
                    encoding = "UTF_32LE";
                } else if (buffer[1] == (byte) 0x00 && buffer[2] == (byte) 0x3F
                        && buffer[3] == (byte) 0x00) {
                    encoding = "UnicodeLittleUnmarked";
                } else if (buffer[1] == (byte) 0x3F && buffer[2] == (byte) 0x78
                        && buffer[3] == (byte) 0x6D) {
                    encoding = "ASCII";
                }
                break;
            case (byte) 0xFE:
                if (buffer[1] == (byte) 0xFF) {
                    encoding = "UnicodeBigUnmarked";
                    ignoreBytes = 2;
                }
                break;
            case (byte) 0xEF:
                if (buffer[1] == (byte) 0xBB && buffer[2] == (byte) 0xBF) {
                    encoding = "UTF8";
                    ignoreBytes = 3;
                }
                break;
            case (byte) 0x4C:
                if (buffer[1] == (byte) 0x6F && buffer[2] == (byte) 0xA7
                        && buffer[3] == (byte) 0x94) {
                    encoding = "CP037";
                }
                break;
        }
        if (encoding == null) {
            encoding = System.getProperty("UTF-8");
        }
        if (readEncoding) {
            read = in.read(buffer, 4, buffer.length - 4);
            Charset cs = Charset.forName(encoding);
            String s = new String(buffer, 4, read, cs);
            int pos = s.indexOf("encoding");
            if (pos == -1) {
                encoding = System.getProperty("UTF-8");
            } else {
                char delim;
                int start = s.indexOf(delim = '\'', pos);
                if (start == -1)
                    start = s.indexOf(delim = '"', pos);
                if (start == -1)
                    throw new IOException("Error detecting XML encoding (1)");
                int end = s.indexOf(delim, start + 1);
                if (end == -1)
                    throw new IOException("Error detecting XML encoding (2)");
                encoding = s.substring(start + 1, end);
            }
        }

        in.reset();
        while (ignoreBytes-- > 0)
            //noinspection ResultOfMethodCallIgnored
            in.read();
        return encoding;
    }
}

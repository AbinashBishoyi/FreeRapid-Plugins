package cz.vity.freerapid.plugins.container.impl;

import org.apache.commons.codec.binary.Hex;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author ntoskrnl
 */
class HexInputStream extends FilterInputStream {

    public HexInputStream(final InputStream is) {
        super(is);
    }

    @Override
    public int read() throws IOException {
        final byte[] b = new byte[2];
        final int len = read(b, 0, 2);
        if (len <= 0) {
            return -1;
        }
        return b[0];
    }

    @Override
    public int read(final byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(final byte[] b, final int off, int len) throws IOException {
        len = in.read(b, off, len);
        if (len <= 0) {
            return len;
        }
        final char[] c = new char[len];
        for (int i = 0, j = off; i < len; i++, j++) {
            c[i] = (char) b[j];
        }
        final byte[] hex;
        try {
            hex = Hex.decodeHex(c);
        } catch (Exception e) {
            throw new IOException("Invalid hex stream", e);
        }
        System.arraycopy(hex, 0, b, off, hex.length);
        return hex.length;
    }

}

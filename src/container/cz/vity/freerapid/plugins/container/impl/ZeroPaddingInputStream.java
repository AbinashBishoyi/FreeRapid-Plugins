package cz.vity.freerapid.plugins.container.impl;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Removes zero padding from the underlying stream.
 */
class ZeroPaddingInputStream extends FilterInputStream {

    public ZeroPaddingInputStream(final InputStream is) {
        super(is);
    }

    private final byte[] buffer = new byte[16];
    private byte oneByte = -2;
    private int pos = buffer.length;

    @Override
    public int read() throws IOException {
        if (pos >= buffer.length) {
            fillBuffer();
        }
        return buffer[pos++];
    }

    protected void fillBuffer() throws IOException {
        if (oneByte >= -1) {
            buffer[0] = oneByte;
        } else {
            buffer[0] = (byte) in.read();
        }
        for (int i = 1; i < buffer.length; i++) {
            buffer[i] = (byte) in.read();
        }
        oneByte = (byte) in.read();
        if (oneByte < 0) {
            for (int i = buffer.length; i > 0; i--) {
                if (buffer[i - 1] != 0) {
                    Arrays.fill(buffer, i, buffer.length, (byte) -1);
                    break;
                }
            }
        }
        pos = 0;
    }

    @Override
    public int read(final byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(final byte[] b, final int off, int len) throws IOException {
        int i = 0;
        for (; i < len; i++) {
            int a = read();
            if (a == -1) {
                break;
            }
            b[i] = (byte) a;
        }
        return i == 0 ? -1 : i;
    }

}

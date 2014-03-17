package cz.vity.freerapid.plugins.container.impl;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Adds zero padding to the underlying stream.
 */
class ZeroPaddingOutputStream extends FilterOutputStream {

    public ZeroPaddingOutputStream(final OutputStream os) {
        super(os);
    }

    private long written = 0;

    @Override
    public void write(final int b) throws IOException {
        written++;
        out.write(b);
    }

    @Override
    public void write(final byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        written += len;
        out.write(b, off, len);
    }

    /**
     * Applies padding and flushes the underlying stream.
     * May produce undesired results if called more than once.
     *
     * @throws IOException if something goes wrong
     */
    @Override
    public void flush() throws IOException {
        final int pad = Math.abs((int) (written & 15));
        if (pad > 0) {
            write(new byte[16 - pad]);
        }
        out.flush();
    }

    @Override
    public void close() throws IOException {
        flush();
        out.close();
    }

}

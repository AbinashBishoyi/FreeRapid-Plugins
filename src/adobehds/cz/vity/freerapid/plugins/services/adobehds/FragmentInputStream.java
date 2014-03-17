package cz.vity.freerapid.plugins.services.adobehds;

import org.apache.commons.httpclient.HttpMethod;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author ntoskrnl
 */
class FragmentInputStream extends InputStream {

    private static final int BOX_TYPE_MDAT = 0x6d646174;

    private final HttpMethod method;
    private final DataInputStream in;
    private int dataAvailable = -1;

    public FragmentInputStream(final HttpMethod method, final InputStream in) {
        this.method = method;
        this.in = new DataInputStream(in);
    }

    @Override
    public int read() throws IOException {
        final byte[] b = new byte[1];
        final int len = read(b, 0, 1);
        if (len == -1) {
            return -1;
        }
        return b[0] & 0xff;
    }

    @Override
    public int read(final byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public synchronized int read(final byte[] b, final int off, int len) throws IOException {
        if (dataAvailable == -1) {
            init();
        }
        if (dataAvailable <= 0) {
            return -1;
        }
        len = Math.min(len, dataAvailable);
        final int bytesRead = in.read(b, off, len);
        if (bytesRead == -1) {
            throw new IOException("Unexpected EOF");
        }
        dataAvailable -= bytesRead;
        return bytesRead;
    }

    @Override
    public synchronized void close() throws IOException {
        try {
            in.close();
        } finally {
            method.abort();
            method.releaseConnection();
        }
    }

    private void init() throws IOException {
        while (true) {
            final BoxHeader header = BoxHeader.readFrom(in);
            if (header.getBoxType() == BOX_TYPE_MDAT) {
                dataAvailable = header.getBoxSize();
                break;
            } else {
                if (in.skipBytes(header.getBoxSize()) != header.getBoxSize()) {
                    throw new EOFException();
                }
            }
        }
    }

}

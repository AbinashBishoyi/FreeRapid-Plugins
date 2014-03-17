package cz.vity.freerapid.plugins.video2audio;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * @author ntoskrnl
 */
abstract class VideoToAudioInputStream extends InputStream {

    protected final InputStream in;

    private ByteBuffer buffer;
    private boolean finished = false;

    public VideoToAudioInputStream(final InputStream in) {
        this.in = in;
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
    public synchronized int read(final byte[] b, final int off, final int len) throws IOException {
        while (buffer == null || !buffer.hasRemaining()) {
            if (finished) {
                return -1;
            }
            buffer = readFrame();
        }
        final int numToCopy = Math.min(buffer.remaining(), len);
        buffer.get(b, off, numToCopy);
        return numToCopy;
    }

    @Override
    public synchronized void close() throws IOException {
        in.close();
        super.close();
    }

    protected void setFinished() {
        finished = true;
    }

    protected abstract ByteBuffer readFrame() throws IOException;

}

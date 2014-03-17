package cz.vity.freerapid.plugins.container.impl;

import org.apache.commons.codec.binary.Hex;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * @author ntoskrnl
 */
class HexOutputStream extends FilterOutputStream {

    public HexOutputStream(final OutputStream os) {
        super(os);
    }

    @Override
    public void write(final int b) throws IOException {
        // I am too lazy to implement buffering.
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(final byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        final char[] hexChars = Hex.encodeHex(len == b.length ? b : Arrays.copyOfRange(b, off, len));
        final byte[] hexBytes = new byte[hexChars.length];
        for (int i = 0; i < hexChars.length; i++) {
            hexBytes[i] = (byte) hexChars[i];
        }
        out.write(hexBytes, 0, hexBytes.length);
    }

}

package cz.vity.freerapid.plugins.video2audio;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author ntoskrnl
 */
public class FlvReader {

    private static final int FLV_MAGIC = 0x464c5601;
    private static final int AAC_PROFILE = 2;
    private static final int TAG_TYPE_AUDIO = 0x08;
    private static final int CODEC_ID_AAC = 10 << 4;
    private static final int CODEC_ID_MP3 = 2 << 4;

    private final DataInputStream in;
    private byte[] info;
    private boolean aac;

    public FlvReader(final InputStream in) throws IOException {
        this.in = new DataInputStream(in);
        checkHeader();
    }

    private void checkHeader() throws IOException {
        if (in.readInt() != FLV_MAGIC
                || (in.readUnsignedByte() & ~0x05) != 0
                || in.readInt() != 0x09
                || in.readInt() != 0) {
            throw new IOException("Invalid FLV stream");
        }
    }

    public byte[] nextAudioPacket() throws IOException {
        while (true) {
            final int type = in.read();
            if (type == -1) {
                return null;
            }
            int size = readInt24();
            skip(7);
            if (size == 0) {
                skip(4);
                return new byte[0];
            }
            if (type != TAG_TYPE_AUDIO) {
                skip(size + 4);
                continue;
            }
            final int flags = in.readUnsignedByte();
            size--;
            final int codecId = flags & 0xf0;
            switch (codecId) {
                case CODEC_ID_AAC:
                    final int aacType = in.readUnsignedByte();
                    size--;
                    if (info == null) {
                        aac = true;
                        final int sampleRate = 44100 << ((flags & 0x0c) >>> 2) >>> 3;
                        final boolean stereo = (flags & 0x01) != 0;
                        initDecoderSpecificInfo(sampleRate, stereo);
                    }
                    if (aacType == 0) {
                        skip(size + 4);
                        continue;
                    }
                    break;
                case CODEC_ID_MP3:
                    break;
                default:
                    throw new IOException("Unsupported codec: 0x" + Integer.toHexString(codecId));
            }
            final byte[] data = new byte[size];
            in.readFully(data);
            skip(4);
            return data;
        }
    }

    public byte[] getDecoderSpecificInfo() {
        return info;
    }

    public boolean isAac() {
        return aac;
    }

    private void initDecoderSpecificInfo(final int sampleFrequency, final boolean stereo) {
        //5 bits profile, 4 bits sample frequency, 4 bits channel configuration
        info = new byte[2];
        info[0] = (byte) (AAC_PROFILE << 3);
        info[0] |= (sampleFrequency >> 1) & 0x7;
        info[1] = (byte) ((sampleFrequency & 0x1) << 7);
        info[1] |= ((stereo ? 2 : 1) << 3);
    }

    private int readInt24() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        if ((ch1 | ch2 | ch3) < 0)
            throw new EOFException();
        return (ch1 << 16) | (ch2 << 8) | ch3;
    }

    private void skip(final int num) throws IOException {
        if (in.skipBytes(num) != num) {
            throw new EOFException();
        }
    }

}

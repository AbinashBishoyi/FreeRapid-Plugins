package cz.vity.freerapid.plugins.services.adobehds;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

/**
 * @author ntoskrnl
 */
public class HdsInputStream extends InputStream {

    private static final Logger logger = Logger.getLogger(HdsInputStream.class.getName());

    private static final int TAG_TYPE_AUDIO = 0x08;
    private static final int TAG_TYPE_VIDEO = 0x09;
    private static final int TAG_TYPE_SCRIPT = 0x12;
    private static final int CODEC_ID_AAC = 0x0a;
    private static final int CODEC_ID_AVC = 0x07;
    private static final int SEQUENCE_HEADER = 0x00;
    private static final int FRAME_TYPE_INFO = 0x05;
    private static final int FLV_PACKET_HEADER_SIZE = 11;

    private final FragmentRequester requester;
    private final ByteBuffer currentPacket = ByteBuffer.allocate(1024 * 1024);
    private DataInputStream currentStream;
    private boolean finished;

    private boolean aacHeaderWritten;
    private boolean avcHeaderWritten;

    public HdsInputStream(final FragmentRequester requester) {
        this.requester = requester;
        currentPacket.put(getFlvHeader());
        currentPacket.flip();
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
        while (!currentPacket.hasRemaining()) {
            if (finished) {
                return -1;
            }
            readPacket();
        }
        final int numToCopy = Math.min(currentPacket.remaining(), len);
        currentPacket.get(b, off, numToCopy);
        return numToCopy;
    }

    @Override
    public synchronized void close() throws IOException {
        if (currentStream != null) {
            currentStream.close();
        }
    }

    private void readPacket() throws IOException {
        while (true) {
            int type;
            while (currentStream == null || (type = currentStream.read()) == -1) {
                close();
                final InputStream stream = requester.nextFragment();
                if (stream == null) {
                    finished = true;
                    return;
                }
                currentStream = new DataInputStream(stream);
            }
            currentPacket.clear();
            final int dataSize = readInt24();
            final int time = readInt24() | (currentStream.readUnsignedByte() << 24);
            final int streamId = readInt24();
            currentPacket.put((byte) type);
            writeInt24(dataSize);
            writeInt24(time & 0xffffff);
            currentPacket.put((byte) (time >>> 24));
            writeInt24(streamId);
            switch (type) {
                case TAG_TYPE_AUDIO: {
                    final int frameInfo = currentStream.readUnsignedByte();
                    currentPacket.put((byte) frameInfo);
                    final int codecId = (frameInfo & 0xf0) >>> 4;
                    if (codecId == CODEC_ID_AAC) {
                        final int aacType = currentStream.readUnsignedByte();
                        currentPacket.put((byte) aacType);
                        if (aacType == SEQUENCE_HEADER) {
                            if (aacHeaderWritten) {
                                logger.info("Skipping AAC sequence header");
                                skipBytes(dataSize - 2 + 4);
                                continue;
                            }
                            aacHeaderWritten = true;
                            logger.info("Writing AAC sequence header");
                        }
                    }
                    break;
                }
                case TAG_TYPE_VIDEO: {
                    final int frameInfo = currentStream.readUnsignedByte();
                    currentPacket.put((byte) frameInfo);
                    final int frameType = (frameInfo & 0xf0) >>> 4;
                    if (frameType == FRAME_TYPE_INFO) {
                        skipBytes(dataSize - 1 + 4);
                        continue;
                    }
                    final int codecId = frameInfo & 0x0f;
                    if (codecId == CODEC_ID_AVC) {
                        final int avcType = currentStream.readUnsignedByte();
                        currentPacket.put((byte) avcType);
                        if (avcType == SEQUENCE_HEADER) {
                            if (avcHeaderWritten) {
                                logger.info("Skipping AVC sequence header");
                                skipBytes(dataSize - 2 + 4);
                                continue;
                            }
                            avcHeaderWritten = true;
                            logger.info("Writing AVC sequence header");
                        }
                    }
                    break;
                }
                case 10:
                case 11: {
                    throw new IOException("Akamai DRM not supported");
                }
                case TAG_TYPE_SCRIPT: {
                    skipBytes(dataSize + 4);
                    continue;
                }
                case 40:
                case 41: {
                    throw new IOException("FlashAccess DRM not supported");
                }
                default: {
                    throw new IOException("Unknown packet type: 0x" + Integer.toHexString(type));
                }
            }
            final int size = dataSize - (currentPacket.position() - FLV_PACKET_HEADER_SIZE);
            if (size > currentPacket.capacity() - currentPacket.position()) {
                throw new IOException("Packet buffer size too small");
            }
            currentStream.readFully(currentPacket.array(), currentPacket.position(), size);
            currentPacket.position(currentPacket.position() + size);
            skipBytes(4);
            currentPacket.putInt(dataSize + FLV_PACKET_HEADER_SIZE);
            currentPacket.flip();
            return;
        }
    }

    private int readInt24() throws IOException {
        int ch1 = currentStream.read();
        int ch2 = currentStream.read();
        int ch3 = currentStream.read();
        if ((ch1 | ch2 | ch3) < 0)
            throw new EOFException();
        return (ch1 << 16) | (ch2 << 8) | ch3;
    }

    private void writeInt24(final int i) {
        currentPacket.put((byte) (i >>> 16));
        currentPacket.put((byte) (i >>> 8));
        currentPacket.put((byte) i);
    }

    private void skipBytes(final int num) throws IOException {
        if (currentStream.skipBytes(num) != num) {
            throw new EOFException();
        }
    }

    private static byte[] getFlvHeader() {
        return new byte[]{'F', 'L', 'V', 0x01, 0x05, 0x00, 0x00, 0x00, 0x09, 0x00, 0x00, 0x00, 0x00};
    }

}

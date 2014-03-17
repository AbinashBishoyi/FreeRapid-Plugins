package cz.vity.freerapid.plugins.services.rtmp;

import org.apache.mina.core.buffer.IoBuffer;

import java.util.logging.Level;
import java.util.logging.Logger;

import static cz.vity.freerapid.plugins.services.rtmp.Header.Type.*;

/**
 * @author Peter Thomas
 */
public class Packet {

    private static final Logger logger = Logger.getLogger(Packet.class.getName());

    public static enum Type implements ByteToEnum.Convert {

        CHUNK_SIZE(0x01),
        // unknown 0x02
        BYTES_READ(0x03),
        CONTROL_MESSAGE(0x04),
        SERVER_BANDWIDTH(0x05),
        CLIENT_BANDWIDTH(0x06),
        // unknown 0x07
        AUDIO_DATA(0x08),
        VIDEO_DATA(0x09),
        // unknown 0x0A - 0x0E
        FLEX_STREAM_SEND(0x0F),
        FLEX_SHARED_OBJECT(0x10),
        FLEX_MESSAGE(0x11),
        NOTIFY(0x12),
        SHARED_OBJECT(0x13),
        INVOKE(0x14),
        FLV_DATA(0x16);

        private final byte value;

        private Type(int value) {
            this.value = (byte) value;
        }

        public byte byteValue() {
            return value;
        }

        private static ByteToEnum<Type> converter = new ByteToEnum<Type>(Type.values());

        public static Type parseByte(byte b) {
            return converter.parseByte(b);
        }

        @Override
        public String toString() {
            return converter.toString(this);
        }

    }

    private Header header;
    private IoBuffer data;
    private boolean complete;

    public Packet() {
    }

    public Packet(Header header, IoBuffer data) {
        this.header = header;
        data.flip();
        this.data = data;
        header.setSize(data.limit());
    }

    public Packet(Header header, int dataSize) {
        this.header = header;
        data = IoBuffer.allocate(dataSize);
    }

    public static Packet bytesRead(long value) {
        Header header = new Header(MEDIUM, 2, Type.BYTES_READ);
        IoBuffer body = IoBuffer.allocate(4);
        body.putInt((int) (value & 0xfffffffL)); // 2^28 - 1
        return new Packet(header, body);
    }

    public static Packet serverBw(int value) {
        Header header = new Header(LARGE, 2, Type.SERVER_BANDWIDTH);
        IoBuffer body = IoBuffer.allocate(8);
        body.putInt(value);
        return new Packet(header, body);
    }

    public static Packet ping(int type, int target, int bufferTime) {
        Header header = new Header(MEDIUM, 2, Type.CONTROL_MESSAGE);
        IoBuffer body = IoBuffer.allocate(10);
        body.putShort((short) type);
        body.putInt(target);
        if (type == 3) {
            body.putInt(bufferTime);
        }
        return new Packet(header, body);
    }

    public static Packet swfVerification(byte[] bytes) {
        Header header = new Header(MEDIUM, 2, Type.CONTROL_MESSAGE);
        IoBuffer body = IoBuffer.allocate(44);
        body.putShort((short) 0x001B);
        body.put(bytes);
        return new Packet(header, body);
    }

    public Header getHeader() {
        return header;
    }

    public IoBuffer getData() {
        return data;
    }

    public void setData(IoBuffer data) {
        this.data = data;
    }

    public boolean isComplete() {
        return complete;
    }

    public boolean decode(IoBuffer in, RtmpSession session) {

        final int position = in.position();

        header = new Header();

        if (!header.decode(in, session)) {
            return false;
        }

        final int channelId = header.getChannelId();
        Packet prevPacket = session.getPrevPacketsIn().get(channelId);

        int toReadRemaining = header.getSize();
        if (prevPacket != null) {
            toReadRemaining -= prevPacket.data.position();
        }

        final int chunkSize = session.getInChunkSize();
        final int toReadNow = toReadRemaining > chunkSize ? chunkSize : toReadRemaining;

        if (in.remaining() < toReadNow) {
            return false;
        }

        session.getPrevHeadersIn().put(channelId, header);

        boolean isNewPacket = false; // just for debugging

        if (prevPacket == null) {
            isNewPacket = true;
            prevPacket = new Packet(header, header.getSize());
            session.getPrevPacketsIn().put(channelId, prevPacket);
        } else {
            header.setRelative(prevPacket.header.isRelative());
        }

        if (logger.isLoggable(Level.FINE)) {
            byte[] bytes = new byte[in.position() - position];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = in.get(position + i);
            }
            if (isNewPacket) {
                logger.fine("====================");
                logger.fine("starting new header: " + header + " <-- " + Utils.toHex(bytes));
            } else {
                logger.fine("resumed prev header: " + header + " <-- " + Utils.toHex(bytes)
                        + "<-- " + prevPacket.header);
            }
        }

        data = prevPacket.data;
        byte[] bytes = new byte[toReadNow];
        in.get(bytes);
        data.put(bytes);

        if (data.position() == header.getSize()) {
            complete = true;
            session.getPrevPacketsIn().remove(channelId);
            data.flip();
        }

        return true;
    }

    public IoBuffer encode(final int chunkSize) {
        IoBuffer buffer = IoBuffer.allocate(2048);
        header.encode(buffer);
        int remaining = header.getSize();
        while (true) {
            final int toWrite = remaining > chunkSize ? chunkSize : remaining;
            byte[] bytes = new byte[toWrite];
            data.get(bytes);
            buffer.put(bytes);
            remaining -= chunkSize;
            if (remaining > 0) {
                Header tiny = new Header(TINY, header.getChannelId(), header.getPacketType());
                tiny.encode(buffer);
            } else {
                break;
            }
        }
        buffer.flip();
        return buffer;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[header: ").append(header);
        sb.append(", complete: ").append(complete);
        sb.append(", data: ").append(data);
        sb.append(']');
        return sb.toString();
    }

}
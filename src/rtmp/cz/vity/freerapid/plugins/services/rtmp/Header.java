package cz.vity.freerapid.plugins.services.rtmp;

import org.apache.mina.core.buffer.IoBuffer;

import java.util.logging.Logger;

/**
 * @author Peter Thomas
 */
class Header {

    private static final Logger logger = Logger.getLogger(Header.class.getName());

    public static enum Type implements ByteToEnum.Convert {

        LARGE(0x00, 12),
        MEDIUM(0x01, 8),
        SMALL(0x02, 4),
        TINY(0x03, 1);

        private final byte value;
        private final int size;

        private Type(int value, int size) {
            this.value = (byte) value;
            this.size = size;
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

    private Type headerType;
    private int channelId;
    private int time;
    private int size;
    private Packet.Type packetType;
    private int streamId;
    private boolean relative = true;

    public Header() {
    }

    public Header(Type headerType, int channelId, Packet.Type packetType) {
        this.headerType = headerType;
        this.channelId = channelId;
        this.packetType = packetType;
    }

    public Type getHeaderType() {
        return headerType;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public Packet.Type getPacketType() {
        return packetType;
    }

    public void setPacketType(Packet.Type packetType) {
        this.packetType = packetType;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getStreamId() {
        return streamId;
    }

    public void setStreamId(int streamId) {
        this.streamId = streamId;
    }

    public int getChannelId() {
        return channelId;
    }

    public boolean isRelative() {
        return relative;
    }

    public void setRelative(boolean relative) {
        this.relative = relative;
    }

    public boolean decode(IoBuffer in, RtmpSession session) {

        final int remaining = in.remaining();

        if (remaining < 1) {
            return false;
        }

        final byte firstByte = in.get();

        final int typeAndChannel;
        final int markerSize;

        if ((firstByte & 0x3f) == 0) {
            if (remaining < 2) {
                return false;
            }
            markerSize = 2;
            typeAndChannel = ((int) firstByte & 0xff) << 8 | ((int) in.get() & 0xff);
        } else if ((firstByte & 0x3f) == 1) {
            if (remaining < 3) {
                return false;
            }
            markerSize = 3;
            typeAndChannel = ((int) firstByte & 0xff) << 16 | ((int) in.get() & 0xff) << 8 | ((int) in.get() & 0xff);
        } else {
            markerSize = 1;
            typeAndChannel = (int) firstByte & 0xff;
        }

        if (markerSize == 1) {
            channelId = (typeAndChannel & 0x3f);
        } else if (markerSize == 2) {
            channelId = 64 + (typeAndChannel & 0xff);
        } else {
            channelId = 64 + ((typeAndChannel >> 8) & 0xff) + ((typeAndChannel & 0xff) << 8);
        }

        final byte headerTypeByte;

        if (markerSize == 1) {
            headerTypeByte = (byte) (typeAndChannel >> 6);
        } else if (markerSize == 2) {
            headerTypeByte = (byte) (typeAndChannel >> 14);
        } else {
            headerTypeByte = (byte) (typeAndChannel >> 22);
        }

        headerType = Header.Type.parseByte(headerTypeByte);

        if (remaining < markerSize + headerType.size - 1) {
            return false;
        }

        final Header prevHeader = session.getPrevHeadersIn().get(channelId);

        // TODO handle 'extended' time values greater than 3 bytes
        switch (headerType) {
            case LARGE:
                time = Utils.readInt24(in);
                size = Utils.readInt24(in);
                packetType = Packet.Type.parseByte(in.get());
                streamId = Utils.readInt32Reverse(in);
                relative = false;
                break;
            case MEDIUM:
                time = Utils.readInt24(in);
                size = Utils.readInt24(in);
                packetType = Packet.Type.parseByte(in.get());
                streamId = prevHeader.streamId;
                break;
            case SMALL:
                time = Utils.readInt24(in);
                size = prevHeader.size;
                packetType = prevHeader.packetType;
                streamId = prevHeader.streamId;
                break;
            case TINY:
                time = prevHeader.time;
                size = prevHeader.size;
                packetType = prevHeader.packetType;
                streamId = prevHeader.streamId;
                break;
        }
        return true;
    }

    public void encode(IoBuffer out) {
        if (channelId <= 63) {
            out.put((byte) ((headerType.value << 6) + channelId));
        } else if (channelId <= 320) {
            out.put((byte) (headerType.value << 6));
            out.put((byte) (channelId - 64));
        } else {
            out.put((byte) ((headerType.value << 6) | 1));
            int tempChannelId = channelId - 64;
            out.put((byte) (tempChannelId & 0xff));
            out.put((byte) (tempChannelId >> 8));
        }
        switch (headerType) {
            case LARGE:
                Utils.writeInt24(out, time);
                Utils.writeInt24(out, size);
                out.put(packetType.byteValue());
                Utils.writeInt32Reverse(out, streamId);
                break;
            case MEDIUM:
                Utils.writeInt24(out, time);
                Utils.writeInt24(out, size);
                out.put(packetType.byteValue());
                break;
            case SMALL:
                Utils.writeInt24(out, time);
                break;
            case TINY:
                break;
        }
        if (RtmpSession.DEBUG) {
            byte[] bytes = new byte[out.position()];
            out.rewind();
            out.get(bytes);
            logger.finest("encoded header: " + toString() + " --> " + Utils.toHex(bytes));
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(headerType);
        sb.append(" c").append(channelId);
        sb.append(" t").append(time);
        if (!relative) {
            sb.append("(a)");
        }
        sb.append(" s").append(size);
        sb.append(" #").append(streamId);
        sb.append(" ").append(packetType).append(']');
        return sb.toString();
    }

}
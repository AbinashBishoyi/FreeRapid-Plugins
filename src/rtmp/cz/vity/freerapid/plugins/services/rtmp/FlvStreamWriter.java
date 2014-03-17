package cz.vity.freerapid.plugins.services.rtmp;

import org.apache.mina.core.buffer.IoBuffer;

import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.logging.Logger;

/**
 * @author Peter Thomas
 * @author ntoskrnl
 */
class FlvStreamWriter implements OutputWriter {

    private static final Logger logger = Logger.getLogger(FlvStreamWriter.class.getName());

    private final RtmpSession session;
    private final WriterStatus status;
    private final WritableByteChannel channel;
    private final PipedInputStream in;
    private final PipedOutputStream out;
    private final IoBuffer buffer;

    private boolean headerWritten = false;
    private int lastAudioTime = -1;
    private int lastVideoTime = -1;

    public FlvStreamWriter(int seekTime, RtmpSession session) {
        this.session = session;
        this.status = new WriterStatus(seekTime, session);
        try {
            out = new PipedOutputStream();
            channel = Channels.newChannel(out);
            in = new PipedInputStream(out, 2048);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        buffer = IoBuffer.allocate(2048);
        buffer.setAutoExpand(true);
    }

    public WriterStatus getStatus() {
        return status;
    }

    public InputStream getStream() {
        return in;
    }

    public synchronized void close() {
        try {
            channel.close();
            out.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        status.logFinalVideoDuration();
    }

    private void writeHeader() {
        IoBuffer buffer = IoBuffer.allocate(13);
        buffer.put((byte) 0x46); // F
        buffer.put((byte) 0x4C); // L
        buffer.put((byte) 0x56); // V
        buffer.put((byte) 0x01); // version
        buffer.put((byte) 0x05); // flags: audio + video
        buffer.putInt(0x09); // header size = 9
        buffer.putInt(0); // previous tag size, here = 0
        buffer.flip();
        write(buffer);
    }

    public synchronized void write(Packet packet) {
        Header header = packet.getHeader();
        int time = status.getChannelAbsoluteTime(header);
        write(header.getPacketType(), packet.getData(), time);
    }

    public synchronized void writeFlvData(IoBuffer data) {
        while (data.hasRemaining()) {
            Packet.Type packetType = Packet.Type.parseByte(data.get());
            int size = Utils.readInt24(data);
            int timestamp = Utils.readInt24(data);
            status.updateVideoChannelTime(timestamp);
            data.getInt(); // 4 bytes of zeros (reserved)
            byte[] bytes = new byte[size];
            data.get(bytes);
            IoBuffer temp = IoBuffer.wrap(bytes);
            write(packetType, temp, timestamp);
            data.getInt(); // FLV tag size (size + 11)
        }
    }

    public synchronized void write(Packet.Type packetType, IoBuffer data, final int time) {
        if (session.getPauseMode() == PauseMode.RESUMING) {
            if (time <= session.getPauseTimestamp()) {
                logger.info("Skipping packet");
                return;
            } else {
                session.setPauseMode(PauseMode.PLAYING);
            }
        }
        if (packetType == Packet.Type.AUDIO_DATA) {
            if (time < lastAudioTime) { //  "<=" does not work
                logger.info("Skipping duplicate audio data packet");
                return;
            }
            lastAudioTime = time;
        }
        if (packetType == Packet.Type.VIDEO_DATA) {
            if (time < lastVideoTime) {  //  "<=" does not work
                logger.info("Skipping duplicate video data packet");
                return;
            }
            lastVideoTime = time;
        }
        logger.fine(String.format("writing FLV tag %s %s %s", packetType, time, data));
        buffer.clear();
        buffer.put(packetType.byteValue());
        final int size = data.limit();
        Utils.writeInt24(buffer, size);
        Utils.writeInt24(buffer, time);
        buffer.putInt(0); // 4 bytes of zeros (reserved)
        buffer.flip();
        write(buffer);
        write(data);
        //==========
        buffer.clear();
        buffer.putInt(size + 11); // previous tag size
        buffer.flip();
        write(buffer);
    }

    private void write(IoBuffer buffer) {
        if (!headerWritten) {
            headerWritten = true;
            logger.info("First data packet received, writing FLV header");
            writeHeader();
        }
        try {
            channel.write(buffer.buf());
        } catch (Exception e) {
            if ("Pipe closed".equals(e.getMessage())) {
                logger.info("Pipe closed, skipping packet");
            } else {
                throw new RuntimeException(e);
            }
        }
    }

}
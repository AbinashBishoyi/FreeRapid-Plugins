package cz.vity.freerapid.plugins.video2audio;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * @author ntoskrnl
 */
public class FlvToMp3InputStream extends VideoToAudioInputStream {

    private FlvReader flvReader;
    private AacToMp3Converter converter;

    public FlvToMp3InputStream(final InputStream in) {
        super(in);
    }

    @Override
    protected ByteBuffer readFrame() throws IOException {
        final byte[] packet;
        if (flvReader == null) {
            packet = init();
        } else {
            packet = flvReader.nextAudioPacket();
        }
        if (packet == null) {
            setFinished();
            if (converter != null) {
                return converter.finish();
            } else {
                return ByteBuffer.allocate(0);
            }
        }
        if (converter != null) {
            return converter.convert(packet);
        } else {
            return ByteBuffer.wrap(packet);
        }
    }

    private byte[] init() throws IOException {
        flvReader = new FlvReader(in);
        final byte[] packet = flvReader.nextAudioPacket();
        if (flvReader.isAac()) {
            converter = new AacToMp3Converter(flvReader.getDecoderSpecificInfo());
        }
        return packet;
    }

}

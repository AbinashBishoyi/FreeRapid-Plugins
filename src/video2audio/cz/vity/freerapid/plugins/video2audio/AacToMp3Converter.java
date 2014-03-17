package cz.vity.freerapid.plugins.video2audio;

import net.sourceforge.jaad.aac.Decoder;
import net.sourceforge.jaad.aac.SampleBuffer;
import net.sourceforge.jump3r.lowlevel.LameEncoder;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author ntoskrnl
 */
public class AacToMp3Converter {

    private final Decoder decoder;
    private final SampleBuffer sampleBuffer = new SampleBuffer();
    private final int targetBitrate;
    private LameEncoder encoder;

    private final byte[] outputBuffer = new byte[4 * 1024];

    public AacToMp3Converter(final byte[] decoderSpecificInfo, final int targetBitrate) throws IOException {
        this.targetBitrate = targetBitrate;
        decoder = new Decoder(decoderSpecificInfo);
    }

    public ByteBuffer convert(final byte[] aacFrame) throws IOException {
        decoder.decodeFrame(aacFrame, sampleBuffer);
        try {
            if (encoder == null) {
                final AudioFormat sourceFormat = new AudioFormat(sampleBuffer.getSampleRate(), sampleBuffer.getBitsPerSample(), sampleBuffer.getChannels(), true, sampleBuffer.isBigEndian());
                encoder = new LameEncoder(sourceFormat, targetBitrate, LameEncoder.CHANNEL_MODE_AUTO, LameEncoder.QUALITY_HIGH, false);
            }
            final int outputLength = encoder.encodeBuffer(sampleBuffer.getData(), 0, sampleBuffer.getData().length, outputBuffer);
            return ByteBuffer.wrap(outputBuffer, 0, outputLength).asReadOnlyBuffer();
        } catch (final Exception e) {
            throw new IOException("MP3 encoding failed", e);
        }
    }

    public ByteBuffer finish() throws IOException {
        int outputLength = 0;
        if (encoder != null) {
            try {
                outputLength = encoder.encodeFinish(outputBuffer);
            } catch (final Exception e) {
                throw new IOException("MP3 encoding failed", e);
            }
        }
        return ByteBuffer.wrap(outputBuffer, 0, outputLength).asReadOnlyBuffer();
    }

}

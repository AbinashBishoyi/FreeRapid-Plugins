package cz.vity.freerapid.plugins.video2audio;

import net.sourceforge.jaad.mp4.MP4Container;
import net.sourceforge.jaad.mp4.api.AudioTrack;
import net.sourceforge.jaad.mp4.api.Frame;
import net.sourceforge.jaad.mp4.api.Movie;
import net.sourceforge.jaad.mp4.api.Track;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * @author ntoskrnl
 */
public class Mp4ToMp3InputStream extends VideoToAudioInputStream {

    private Track track;
    private AacToMp3Converter converter;

    public Mp4ToMp3InputStream(final InputStream in) {
        super(in);
    }

    @Override
    protected ByteBuffer readFrame() throws IOException {
        if (track == null) {
            init();
        }
        final Frame frame = track.readNextFrame();
        if (frame == null) {
            setFinished();
            return converter.finish();
        }
        return converter.convert(frame.getData());
    }

    private void init() throws IOException {
        final MP4Container container = new MP4Container(in);
        final Movie movie = container.getMovie();
        final List<Track> tracks = movie.getTracks(AudioTrack.AudioCodec.AAC);
        if (tracks.isEmpty()) {
            throw new IOException("No AAC tracks found");
        }
        track = tracks.get(0);
        converter = new AacToMp3Converter(track.getDecoderSpecificInfo());
    }

}

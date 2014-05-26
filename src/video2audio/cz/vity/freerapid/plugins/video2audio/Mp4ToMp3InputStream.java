package cz.vity.freerapid.plugins.video2audio;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.TrackBox;
import com.coremedia.iso.boxes.mdat.SampleList;
import com.coremedia.iso.boxes.sampleentry.AudioSampleEntry;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Sample;
import com.googlecode.mp4parser.authoring.SampleImpl;
import com.googlecode.mp4parser.boxes.mp4.ESDescriptorBox;
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.AudioSpecificConfig;
import com.googlecode.mp4parser.util.Path;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;

/**
 * @author ntoskrnl
 * @author tong2shot
 */
public class Mp4ToMp3InputStream extends VideoToAudioInputStream {

    private List<Sample> samples = null;
    private AacToMp3Converter converter;
    private Iterator<Sample> samplesIterator;

    public Mp4ToMp3InputStream(final FileInputStream in, final int targetBitrate) {
        super(in, targetBitrate);
    }

    public Mp4ToMp3InputStream(final FileInputStream in) {
        super(in);
    }

    @Override
    protected ByteBuffer readFrame() throws IOException {
        if (samples == null) {
            init();
        }
        if (!samplesIterator.hasNext()) {
            setFinished();
            return converter.finish();
        }
        return converter.convert(readFrame(samplesIterator.next()));
    }

    public byte[] readFrame(Sample sample) throws IOException {
        in.getChannel().position(((SampleImpl) sample).getOffset());
        byte[] b = new byte[(int) sample.getSize()];
        ByteBuffer bb = ByteBuffer.wrap(b);
        in.getChannel().read(bb);
        return b;
    }

    private void init() throws IOException {
        IsoFile isoFile = new IsoFile(new FileDataSourceImpl(in.getChannel()));
        TrackBox trackBox;
        try {
            trackBox = (TrackBox) Path.getPath(isoFile, "/moov/trak/mdia/minf/stbl/stsd/mp4a/../../../../../");
        } catch (Exception e) {
            throw new IOException("Error getting track box");
        }
        samples = new SampleList(trackBox);
        samplesIterator = samples.iterator();

        List<AudioSampleEntry> aseList;
        AudioSampleEntry mp4aAse = null;
        try {
            aseList = isoFile.getMovieBox().getBoxes(AudioSampleEntry.class, true);
            for (AudioSampleEntry ase : aseList) {
                if (ase.getType().equals("mp4a")) {
                    mp4aAse = ase;
                    break;
                }
            }
        } catch (Exception e) {
            throw new IOException("Error getting audio sample entry");
        }
        if (mp4aAse == null) {
            throw new IOException("'mp4a' audio sample entry not found");
        }
        ESDescriptorBox esDescriptorBox = (ESDescriptorBox) mp4aAse.getBoxes().get(0);
        AudioSpecificConfig asc = esDescriptorBox.getEsDescriptor().getDecoderConfigDescriptor().getAudioSpecificInfo();
        byte[] data = asc.getConfigBytes();
        converter = new AacToMp3Converter(data, targetBitrate);
        in.getChannel().position(0);
    }

}

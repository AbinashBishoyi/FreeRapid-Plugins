package com.googlecode.mp4parser.authoring.tracks;

import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.SampleDescriptionBox;
import com.coremedia.iso.boxes.SoundMediaHeaderBox;
import com.coremedia.iso.boxes.sampleentry.AudioSampleEntry;
import com.googlecode.mp4parser.DataSource;
import com.googlecode.mp4parser.authoring.AbstractTrack;
import com.googlecode.mp4parser.authoring.Sample;
import com.googlecode.mp4parser.authoring.SampleImpl;
import com.googlecode.mp4parser.authoring.TrackMetaData;
import com.googlecode.mp4parser.boxes.mp4.ESDescriptorBox;
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * AAC Track from FLV container
 *
 * @author tong2shot
 */
public class FlvAacTrackImpl extends AbstractTrack {

    private static final int PREV_TAG_SIZE = 4;
    private static final int AUDIO_TAG_TYPE = 0x08;
    private static final int AAC_SOUND_FORMAT = 0x0a;
    private static final int AAC_SEQUENCE_HEADER = 0x00;

    TrackMetaData trackMetaData = new TrackMetaData();
    SampleDescriptionBox sampleDescriptionBox;
    int bufferSizeDB;
    long maxBitRate;
    long avgBitRate;
    long[] decTimes;

    private String lang = "eng";
    private List<Sample> samples;


    public FlvAacTrackImpl(DataSource channel, String lang) throws IOException {
        this.lang = lang;
        parse(channel);
    }

    public FlvAacTrackImpl(DataSource channel) throws IOException {
        parse(channel);
    }

    private void checkHeader(DataSource channel) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(9);
        channel.read(bb);
        BitReaderBuffer brb = new BitReaderBuffer((ByteBuffer) bb.rewind());
        int signature = brb.readBits(24); //"FLV" 0x464c56
        int version = brb.readBits(8); //0x01
        int flags = brb.readBits(8); //0x05, bitmask: 0x04 is audio, 0x01 is video
        int headerSize = brb.readBits(32); //0x09
        if ((signature != 0x464c56) && (version != 0x01) && (headerSize != 0x09)) {
            throw new IOException("Invalid FLV header");
        }
    }

    private void parse(DataSource channel) throws IOException {
        checkHeader(channel);
        samples = new ArrayList<Sample>();
        AacAudioSpecificConfig aacASC = readSamples(channel);

        double packetsPerSecond = (double) AACTrackImpl.samplingFrequencyIndexMap.get(aacASC.samplingFrequencyIndex) / 1024.0;
        double duration = samples.size() / packetsPerSecond;

        long dataSize = 0;
        LinkedList<Integer> queue = new LinkedList<Integer>();
        for (Sample sample : samples) {
            int size = (int) sample.getSize();
            dataSize += size;
            queue.add(size);
            while (queue.size() > packetsPerSecond) {
                queue.pop();
            }
            if (queue.size() == (int) packetsPerSecond) {
                int currSize = 0;
                for (Integer aQueue : queue) {
                    currSize += aQueue;
                }
                double currBitrate = 8.0 * currSize / queue.size() * packetsPerSecond;
                if (currBitrate > maxBitRate) {
                    maxBitRate = (int) currBitrate;
                }
            }
        }

        avgBitRate = (int) (8 * dataSize / duration);

        bufferSizeDB = 1536; /* TODO: Calcultate this somehow! */

        sampleDescriptionBox = new SampleDescriptionBox();
        AudioSampleEntry audioSampleEntry = new AudioSampleEntry("mp4a");
        if (aacASC.channelConfig == 7) {
            audioSampleEntry.setChannelCount(8);
        } else {
            audioSampleEntry.setChannelCount(aacASC.channelConfig);
        }
        audioSampleEntry.setSampleRate(AACTrackImpl.samplingFrequencyIndexMap.get(aacASC.samplingFrequencyIndex));
        audioSampleEntry.setDataReferenceIndex(1);
        audioSampleEntry.setSampleSize(16);


        ESDescriptorBox esds = new ESDescriptorBox();
        ESDescriptor descriptor = new ESDescriptor();
        descriptor.setEsId(0);

        SLConfigDescriptor slConfigDescriptor = new SLConfigDescriptor();
        slConfigDescriptor.setPredefined(2);
        descriptor.setSlConfigDescriptor(slConfigDescriptor);

        DecoderConfigDescriptor decoderConfigDescriptor = new DecoderConfigDescriptor();
        decoderConfigDescriptor.setObjectTypeIndication(0x40);
        decoderConfigDescriptor.setStreamType(5);
        decoderConfigDescriptor.setBufferSizeDB(bufferSizeDB);
        decoderConfigDescriptor.setMaxBitRate(maxBitRate);
        decoderConfigDescriptor.setAvgBitRate(avgBitRate);

        AudioSpecificConfig audioSpecificConfig = new AudioSpecificConfig();
        audioSpecificConfig.setAudioObjectType(2); // AAC LC
        audioSpecificConfig.setSamplingFrequencyIndex(aacASC.samplingFrequencyIndex);
        audioSpecificConfig.setChannelConfiguration(aacASC.channelConfig);
        decoderConfigDescriptor.setAudioSpecificInfo(audioSpecificConfig);

        descriptor.setDecoderConfigDescriptor(decoderConfigDescriptor);

        ByteBuffer data = descriptor.serialize();
        esds.setEsDescriptor(descriptor);
        esds.setData(data);
        audioSampleEntry.addBox(esds);
        sampleDescriptionBox.addBox(audioSampleEntry);

        trackMetaData.setCreationTime(new Date());
        trackMetaData.setModificationTime(new Date());
        trackMetaData.setLanguage(lang);
        trackMetaData.setVolume(1);
        trackMetaData.setTimescale(AACTrackImpl.samplingFrequencyIndexMap.get(aacASC.samplingFrequencyIndex)); // Audio tracks always use sampleRate as timescale
        decTimes = new long[samples.size()];
        Arrays.fill(decTimes, 1024);
    }

    private AacAudioSpecificConfig readSamples(DataSource channel) throws IOException {
        AacAudioSpecificConfig aacASC = null;
        channel.read(ByteBuffer.allocate(PREV_TAG_SIZE));

        outer:
        while (true) {
            ByteBuffer bb = ByteBuffer.allocate(11);
            while (bb.position() < 11) {
                if (channel.read(bb) == -1) {
                    break outer;
                }
            }
            BitReaderBuffer brb = new BitReaderBuffer((ByteBuffer) bb.rewind());

            int tagType = brb.readBits(8);
            int dataSize = brb.readBits(24);
            brb.readBits(32); //timeStamp
            brb.readBits(24); //streamId
            long pos = channel.position();
            if ((tagType != AUDIO_TAG_TYPE) || (dataSize == 0)) {
                channel.position(pos + dataSize + PREV_TAG_SIZE);
                continue;
            }

            bb = ByteBuffer.allocate(2);
            channel.read(bb);
            brb = new BitReaderBuffer((ByteBuffer) bb.rewind());
            int soundFormat = brb.readBits(4);
            if (soundFormat != AAC_SOUND_FORMAT) {
                throw new IOException("Only supports AAC");
            }
            brb.readBits(2); //soundRate
            brb.readBits(1); //soundBitSamples
            brb.readBits(1); //soundType
            int aacPacketType = brb.readBits(8);

            ByteBuffer data = ByteBuffer.allocate(dataSize - 2);
            channel.read(data);
            data.rewind();
            if (aacPacketType == AAC_SEQUENCE_HEADER) {
                if (aacASC == null) {
                    brb = new BitReaderBuffer(data);
                    aacASC = new AacAudioSpecificConfig();
                    aacASC.audioObjectType = brb.readBits(5);
                    aacASC.samplingFrequencyIndex = brb.readBits(4);
                    aacASC.channelConfig = brb.readBits(4);
                }
            } else {
                samples.add(new SampleImpl(data));
            }
            channel.read(ByteBuffer.allocate(PREV_TAG_SIZE));
        }
        return aacASC;
    }

    class AacAudioSpecificConfig {
        int audioObjectType;
        int samplingFrequencyIndex;
        int channelConfig;
    }

    @Override
    public SampleDescriptionBox getSampleDescriptionBox() {
        return sampleDescriptionBox;
    }

    @Override
    public long[] getSampleDurations() {
        return decTimes;
    }

    @Override
    public TrackMetaData getTrackMetaData() {
        return trackMetaData;
    }

    @Override
    public String getHandler() {
        return "soun";
    }

    @Override
    public List<Sample> getSamples() {
        return samples;
    }

    @Override
    public Box getMediaHeaderBox() {
        return new SoundMediaHeaderBox();
    }
}

package cz.vity.freerapid.plugins.services.adobehds;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.IOException;

/**
 * This class doesn't really understand any semantics yet; it just parses the data.
 * I need to see a more complex bootstrap info before I can improve this.
 *
 * @author ntoskrnl
 */
class BootstrapInfo {

    private static final int BOX_TYPE_ABST = 0x61627374;
    private static final int BOX_TYPE_ASRT = 0x61737274;
    private static final int BOX_TYPE_AFRT = 0x61667274;

    private int fragmentCount;

    public BootstrapInfo(final DataInput in) throws IOException {
        final BoxHeader header = BoxHeader.readFrom(in);
        if (header.getBoxType() != BOX_TYPE_ABST) {
            throw new IOException("abst box expected");
        }
        final int version = in.readUnsignedByte();
        final int flags = readInt24(in);
        final int bootstrapInfoVersion = in.readInt();
        final int b = in.readUnsignedByte();
        final int profile = (b & 0xc0) >>> 6;
        final int live = (b & 0x20) >>> 5;
        final int update = (b & 0x10) >>> 4;
        final int timescale = in.readInt();
        final long currentMediaTime = in.readLong();
        final long smpteTimeCodeOffset = in.readLong();
        final String movieIdentifier = readString(in);
        final int serverEntryCount = in.readUnsignedByte();
        for (int i = 0; i < serverEntryCount; i++) {
            readString(in);
        }
        final int qualityEntryCount = in.readUnsignedByte();
        for (int i = 0; i < qualityEntryCount; i++) {
            readString(in);
        }
        final String drmData = readString(in);
        final String metadata = readString(in);
        final int segmentRunTableCount = in.readUnsignedByte();
        for (int i = 0; i < segmentRunTableCount; i++) {
            readSegmentRunTableBox(in);
        }
        final int fragmentRunTableCount = in.readUnsignedByte();
        for (int i = 0; i < fragmentRunTableCount; i++) {
            readFragmentRunTableBox(in);
        }
    }

    public int getFragmentCount() {
        return fragmentCount;
    }

    private void readSegmentRunTableBox(final DataInput in) throws IOException {
        final BoxHeader header = BoxHeader.readFrom(in);
        if (header.getBoxType() != BOX_TYPE_ASRT) {
            throw new IOException("asrt box expected");
        }
        final int version = in.readUnsignedByte();
        final int flags = readInt24(in);
        final int qualityEntryCount = in.readUnsignedByte();
        for (int i = 0; i < qualityEntryCount; i++) {
            readString(in);
        }
        final int segmentRunEntryCount = in.readInt();
        for (int i = 0; i < segmentRunEntryCount; i++) {
            final int firstSegment = in.readInt();
            final int fragmentsPerSegment = in.readInt();
            fragmentCount = fragmentsPerSegment;
        }
    }

    private void readFragmentRunTableBox(final DataInput in) throws IOException {
        final BoxHeader header = BoxHeader.readFrom(in);
        if (header.getBoxType() != BOX_TYPE_AFRT) {
            throw new IOException("afrt box expected");
        }
        final int version = in.readUnsignedByte();
        final int flags = readInt24(in);
        final int timescale = in.readInt();
        final int qualityEntryCount = in.readUnsignedByte();
        for (int i = 0; i < qualityEntryCount; i++) {
            readString(in);
        }
        final int fragmentRunEntryCount = in.readInt();
        for (int i = 0; i < fragmentRunEntryCount; i++) {
            final int firstFragment = in.readInt();
            final long firstFragmentTimestamp = in.readLong();
            final int fragmentDuration = in.readInt();
            if (fragmentDuration == 0) {
                final int discontinuityIndicator = in.readUnsignedByte();
            }
        }
    }

    private int readInt24(final DataInput in) throws IOException {
        int ch1 = in.readUnsignedByte();
        int ch2 = in.readUnsignedByte();
        int ch3 = in.readUnsignedByte();
        return (ch1 << 16) | (ch2 << 8) | ch3;
    }

    private String readString(final DataInput in) throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte b;
        while ((b = in.readByte()) != 0) {
            os.write(b);
        }
        return new String(os.toByteArray(), "UTF-8");
    }

}

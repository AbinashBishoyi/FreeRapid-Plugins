package com.googlecode.mp4parser.boxes;

import com.coremedia.iso.IsoTypeReader;
import com.coremedia.iso.IsoTypeWriter;
import com.googlecode.mp4parser.AbstractFullBox;
import com.googlecode.mp4parser.annotations.DoNotParseDetail;
import com.googlecode.mp4parser.boxes.cenc.CencSampleAuxiliaryDataFormat;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


public abstract class AbstractSampleEncryptionBox extends AbstractFullBox {
    protected int algorithmId = -1;
    protected int ivSize = -1;
    protected byte[] kid = new byte[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
    List<CencSampleAuxiliaryDataFormat> entries = new LinkedList<CencSampleAuxiliaryDataFormat>();

    protected AbstractSampleEncryptionBox(String type) {
        super(type);
    }

    public int getOffsetToFirstIV() {
        if (!isParsed()) {
            parseDetails();
        }
        int offset = (getSize() > (1l << 32) ? 16 : 8);
        offset += isOverrideTrackEncryptionBoxParameters() ? (4 + kid.length) : 0;
        offset += 4; //num entries
        return offset;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);

        if ((getFlags() & 0x1) > 0) {
            algorithmId = IsoTypeReader.readUInt24(content);
            ivSize = IsoTypeReader.readUInt8(content);
            kid = new byte[16];
            content.get(kid);
        }

        long numOfEntries = IsoTypeReader.readUInt32(content);
        ByteBuffer parseEight = content.duplicate();
        ByteBuffer parseSixteen = content.duplicate();

        entries = parseEntries(parseEight, numOfEntries, 8);
        if (entries == null) {
            entries = parseEntries(parseSixteen, numOfEntries, 16);
            content.position(content.position() + content.remaining() - parseSixteen.remaining());
        } else {
            content.position(content.position() + content.remaining() - parseEight.remaining());
        }
        if (entries == null) {
            throw new RuntimeException("Cannot parse SampleEncryptionBox");
        }

    }

    private List<CencSampleAuxiliaryDataFormat> parseEntries(ByteBuffer content, final long numOfEntries, int ivSize) {
        List<CencSampleAuxiliaryDataFormat> _entries = new LinkedList<CencSampleAuxiliaryDataFormat>();
        try {
            long remainingNumOfEntries = numOfEntries;
            while (remainingNumOfEntries-- > 0) {
                CencSampleAuxiliaryDataFormat e = new CencSampleAuxiliaryDataFormat();
                e.iv = new byte[ivSize];
                content.get(e.iv);
                if ((getFlags() & 0x2) > 0) {
                    int numOfPairs = IsoTypeReader.readUInt16(content);
                    e.pairs = new LinkedList<CencSampleAuxiliaryDataFormat.Pair>();
                    while (numOfPairs-- > 0) {
                        e.pairs.add(e.createPair(IsoTypeReader.readUInt16(content), IsoTypeReader.readUInt32(content)));
                    }
                }
                _entries.add(e);
            }
        } catch (BufferUnderflowException bue) {
            return null;
        }
        return _entries;

    }

    public List<CencSampleAuxiliaryDataFormat> getEntries() {
        if (!isParsed()) {
            parseDetails();
        }
        return entries;
    }

    public void setEntries(List<CencSampleAuxiliaryDataFormat> entries) {
        if (!isParsed()) {
            parseDetails();
        }
        this.entries = entries;
    }

    @DoNotParseDetail
    public boolean isSubSampleEncryption() {
        return (getFlags() & 0x2) > 0;
    }

    @DoNotParseDetail
    protected boolean isOverrideTrackEncryptionBoxParameters() {
        return (getFlags() & 0x1) > 0;
    }

    @DoNotParseDetail
    public void setSubSampleEncryption(boolean b) {
        if (b) {
            setFlags(getFlags() | 0x2);
        } else {
            setFlags(getFlags() & (0xffffff ^ 0x2));
        }
    }


    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        if (isOverrideTrackEncryptionBoxParameters()) {
            IsoTypeWriter.writeUInt24(byteBuffer, algorithmId);
            IsoTypeWriter.writeUInt8(byteBuffer, ivSize);
            byteBuffer.put(kid);
        }
        IsoTypeWriter.writeUInt32(byteBuffer, entries.size());
        for (CencSampleAuxiliaryDataFormat entry : entries) {
            if (entry.iv.length != 8 && entry.iv.length != 16) {
                throw new RuntimeException("IV must be either 8 or 16 bytes");
            }
            byteBuffer.put(entry.iv);
            if (isSubSampleEncryption()) {
                IsoTypeWriter.writeUInt16(byteBuffer, entry.pairs.size());
                for (CencSampleAuxiliaryDataFormat.Pair pair : entry.pairs) {
                    IsoTypeWriter.writeUInt16(byteBuffer, pair.clear);
                    IsoTypeWriter.writeUInt32(byteBuffer, pair.encrypted);
                }
            }
        }
    }

    @Override
    protected long getContentSize() {
        long contentSize = 4;
        if (isOverrideTrackEncryptionBoxParameters()) {
            contentSize += 4;
            contentSize += kid.length;
        }
        contentSize += 4;
        for (CencSampleAuxiliaryDataFormat entry : entries) {
            contentSize += entry.getSize();
        }
        return contentSize;
    }

    @Override
    public void getBox(WritableByteChannel os) throws IOException {
        super.getBox(os);
    }

    @Override
    public boolean equals(Object o) {
        if (!isParsed()) {
            parseDetails();
        }
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AbstractSampleEncryptionBox that = (AbstractSampleEncryptionBox) o;

        if (algorithmId != that.algorithmId) {
            return false;
        }
        if (ivSize != that.ivSize) {
            return false;
        }
        if (entries != null ? !entries.equals(that.entries) : that.entries != null) {
            return false;
        }
        if (!Arrays.equals(kid, that.kid)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        if (!isParsed()) {
            parseDetails();
        }
        int result = algorithmId;
        result = 31 * result + ivSize;
        result = 31 * result + (kid != null ? Arrays.hashCode(kid) : 0);
        result = 31 * result + (entries != null ? entries.hashCode() : 0);
        return result;
    }

    public List<Short> getEntrySizes() {
        if (!isParsed()) {
            parseDetails();
        }
        List<Short> entrySizes = new ArrayList<Short>(entries.size());
        for (CencSampleAuxiliaryDataFormat entry : entries) {
            short size = (short) entry.iv.length;
            if (isSubSampleEncryption()) {
                size += 2; //numPairs
                size += entry.pairs.size() * 6;
            }
            entrySizes.add(size);
        }
        return entrySizes;
    }
}

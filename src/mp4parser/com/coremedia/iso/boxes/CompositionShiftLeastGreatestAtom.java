package com.coremedia.iso.boxes;

import com.googlecode.mp4parser.AbstractFullBox;

import java.nio.ByteBuffer;

/**
 * <h1>4cc = "{@value #TYPE}"</h1>
 * <p>
 * The optional composition shift least greatest atom summarizes the calculated
 * minimum and maximum offsets between decode and composition time, as well as
 * the start and end times, for all samples. This allows a reader to determine
 * the minimum required time for decode to obtain proper presentation order without
 * needing to scan the sample table for the range of offsets. The type of the
 * composition shift least greatest atom is ‘cslg’.</p>
 */
public class CompositionShiftLeastGreatestAtom extends AbstractFullBox {
    public static final String TYPE = "cslg";

    public CompositionShiftLeastGreatestAtom() {
        super(TYPE);
    }

    // A 32-bit unsigned integer that specifies the calculated value.
    int compositionOffsetToDisplayOffsetShift;

    // A 32-bit signed integer that specifies the calculated value.
    int leastDisplayOffset;

    // A 32-bit signed integer that specifies the calculated value.
    int greatestDisplayOffset;

    //A 32-bit signed integer that specifies the calculated value.
    int displayStartTime;

    //A 32-bit signed integer that specifies the calculated value.
    int displayEndTime;


    @Override
    protected long getContentSize() {
        return 24;
    }

    @Override
    public void _parseDetails(ByteBuffer content) {
        parseVersionAndFlags(content);
        compositionOffsetToDisplayOffsetShift = content.getInt();
        leastDisplayOffset = content.getInt();
        greatestDisplayOffset = content.getInt();
        displayStartTime = content.getInt();
        displayEndTime = content.getInt();
    }

    @Override
    protected void getContent(ByteBuffer byteBuffer) {
        writeVersionAndFlags(byteBuffer);
        byteBuffer.putInt(compositionOffsetToDisplayOffsetShift);
        byteBuffer.putInt(leastDisplayOffset);
        byteBuffer.putInt(greatestDisplayOffset);
        byteBuffer.putInt(displayStartTime);
        byteBuffer.putInt(displayEndTime);
    }


    public int getCompositionOffsetToDisplayOffsetShift() {
        if (!isParsed()) {
            parseDetails();
        }
        return compositionOffsetToDisplayOffsetShift;
    }

    public void setCompositionOffsetToDisplayOffsetShift(int compositionOffsetToDisplayOffsetShift) {
        if (!isParsed()) {
            parseDetails();
        }
        this.compositionOffsetToDisplayOffsetShift = compositionOffsetToDisplayOffsetShift;
    }

    public int getLeastDisplayOffset() {
        if (!isParsed()) {
            parseDetails();
        }
        return leastDisplayOffset;
    }

    public void setLeastDisplayOffset(int leastDisplayOffset) {
        if (!isParsed()) {
            parseDetails();
        }
        this.leastDisplayOffset = leastDisplayOffset;
    }

    public int getGreatestDisplayOffset() {
        if (!isParsed()) {
            parseDetails();
        }
        return greatestDisplayOffset;
    }

    public void setGreatestDisplayOffset(int greatestDisplayOffset) {
        if (!isParsed()) {
            parseDetails();
        }
        this.greatestDisplayOffset = greatestDisplayOffset;
    }

    public int getDisplayStartTime() {
        if (!isParsed()) {
            parseDetails();
        }
        return displayStartTime;
    }

    public void setDisplayStartTime(int displayStartTime) {
        if (!isParsed()) {
            parseDetails();
        }
        this.displayStartTime = displayStartTime;
    }

    public int getDisplayEndTime() {
        if (!isParsed()) {
            parseDetails();
        }
        return displayEndTime;
    }

    public void setDisplayEndTime(int displayEndTime) {
        if (!isParsed()) {
            parseDetails();
        }
        this.displayEndTime = displayEndTime;
    }
}

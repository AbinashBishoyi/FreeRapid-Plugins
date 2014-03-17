package cz.vity.freerapid.plugins.services.adobehds;

import java.io.DataInput;
import java.io.IOException;

/**
 * @author ntoskrnl
 */
class BoxHeader {

    private final int boxType;
    private final int boxSize;

    private BoxHeader(final int boxType, final int boxSize) {
        this.boxType = boxType;
        this.boxSize = boxSize;
    }

    public int getBoxType() {
        return boxType;
    }

    public int getBoxSize() {
        return boxSize;
    }

    public static BoxHeader readFrom(final DataInput in) throws IOException {
        int boxSize = in.readInt();
        final int boxType = in.readInt();
        if (boxSize == 1) {
            boxSize = (int) in.readLong() - 16;
        } else {
            boxSize -= 8;
        }
        return new BoxHeader(boxType, boxSize);
    }

}

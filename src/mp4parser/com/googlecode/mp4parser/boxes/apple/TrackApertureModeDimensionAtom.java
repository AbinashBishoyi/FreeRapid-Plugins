package com.googlecode.mp4parser.boxes.apple;

import com.googlecode.mp4parser.AbstractContainerBox;

/**
 * <h1>4cc = "{@value #TYPE}"</h1>
 * Don't know what it is but it is obviously a container box.
 */
public class TrackApertureModeDimensionAtom extends AbstractContainerBox {
    public static final String TYPE = "tapt";

    public TrackApertureModeDimensionAtom() {
        super(TYPE);
    }


}

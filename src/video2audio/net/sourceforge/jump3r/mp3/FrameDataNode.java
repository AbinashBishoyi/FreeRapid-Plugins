package net.sourceforge.jump3r.mp3;

public final class FrameDataNode {
    FrameDataNode nxt;
    /**
     * Frame Identifier
     */
    int fid;
    /**
     * 3-character language descriptor
     */
    String lng;

    Inf dsc = new Inf(), txt = new Inf();
}
package cz.vity.freerapid.plugins.services.rtmp;

/**
 * @author ntoskrnl
 */
public interface PacketHandler {

    /**
     * Handle packet.
     *
     * @param packet  packet to handle
     * @param session current session
     * @return true if next handler should be called too, false if not
     */
    public boolean handle(Packet packet, RtmpSession session);

}

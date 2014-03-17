package cz.vity.freerapid.plugins.services.rtmp;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Peter Thomas
 * @author ntoskrnl
 */
class RtmpDecoder extends CumulativeProtocolDecoder {

    private static final Logger logger = Logger.getLogger(RtmpDecoder.class.getName());

    @Override
    protected boolean doDecode(IoSession ioSession, IoBuffer in, ProtocolDecoderOutput _unused) {
        return decode(in, RtmpSession.getFrom(ioSession));
    }

    public static boolean decode(IoBuffer in, RtmpSession session) {

        if (!session.isServerHandshakeReceived()) {
            if (!Handshake.decodeServerResponse(in, session)) {
                return false;
            }
            session.setServerHandshakeReceived(true);
            logger.info("server handshake processed, sending reply");
            session.send(Handshake.generateClientRequest2(session));
            session.send(new Invoke("connect", 3, session.getConnectParams()));
            return true;
        }

        final int position = in.position();
        Packet packet = new Packet();

        if (!packet.decode(in, session)) {
            in.position(position);
            return false;
        }

        if (!packet.isComplete()) { // but finished decoding chunk
            return true;
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("packet complete: " + packet);
        }

        for (PacketHandler handler : session.getPacketHandlers()) {
            packet.getData().rewind();
            if (!handler.handle(packet, session)) {
                break;
            }
        }

        return true;
    }

}
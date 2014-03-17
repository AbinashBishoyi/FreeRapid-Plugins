package cz.vity.freerapid.plugins.services.rtmp;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Peter Thomas
 */
class RtmpEncoder implements ProtocolEncoder {

    private static final Logger logger = Logger.getLogger(RtmpEncoder.class.getName());

    public void encode(IoSession ioSession, Object object, ProtocolEncoderOutput out) {

        RtmpSession session = RtmpSession.getFrom(ioSession);

        if (object instanceof Handshake) {
            Handshake hs = (Handshake) object;
            out.write(hs.getData());

            if (logger.isLoggable(Level.FINE)) {
                if (session.isServerHandshakeReceived()) {
                    logger.fine("sent client handshake part 2: " + hs.getData());
                } else {
                    logger.fine("sent client handshake part 1: " + hs.getData());
                }
            }
            return;
        }

        if (!session.isHandshakeComplete()) {
            logger.info("handshake complete, sending first packet after");
            session.setHandshakeComplete(true);
        }

        Packet packet = (Packet) object;
        IoBuffer buffer = packet.encode(session.getOutChunkSize());
        out.write(buffer);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("sent packet data: " + buffer);
        }

    }

    public void dispose(IoSession session) throws Exception {
    }

}
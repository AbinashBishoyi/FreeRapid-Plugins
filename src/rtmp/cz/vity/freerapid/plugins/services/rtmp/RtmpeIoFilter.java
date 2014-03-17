package cz.vity.freerapid.plugins.services.rtmp;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;

import java.util.logging.Logger;

/**
 * @author Peter Thomas
 */
class RtmpeIoFilter extends IoFilterAdapter {

    private static final Logger logger = Logger.getLogger(RtmpeIoFilter.class.getName());

    @Override
    public void messageReceived(NextFilter nextFilter, IoSession ioSession, Object message) throws Exception {
        RtmpSession session = RtmpSession.getFrom(ioSession);
        final long bytesReadSoFar = ioSession.getReadBytes();
        if (bytesReadSoFar > session.getBytesReadLastSent() + 600 * 1024) {
            logger.info("sending bytes read " + bytesReadSoFar);
            session.send(Packet.bytesRead(bytesReadSoFar));
            session.setBytesReadLastSent(bytesReadSoFar);
        }
        if (!session.isEncrypted() || !session.isHandshakeComplete() || !(message instanceof IoBuffer)) {
            nextFilter.messageReceived(ioSession, message);
            return;
        }
        IoBuffer buf = (IoBuffer) message;
        int initial = buf.position();
        byte[] encrypted = new byte[buf.remaining()];
        buf.get(encrypted);
        byte[] plain = session.getCipherIn().update(encrypted);
        buf.position(initial);
        buf.put(plain);
        buf.position(initial);
        nextFilter.messageReceived(ioSession, buf);
    }

    @Override
    public void filterWrite(NextFilter nextFilter, IoSession ioSession, WriteRequest writeRequest) throws Exception {
        RtmpSession session = RtmpSession.getFrom(ioSession);
        if (!session.isEncrypted() || !session.isHandshakeComplete()) {
            nextFilter.filterWrite(ioSession, writeRequest);
            return;
        }
        IoBuffer buf = (IoBuffer) writeRequest.getMessage();
        if (!buf.hasRemaining()) {
            // ignore empty buffers
            nextFilter.filterWrite(ioSession, writeRequest);
        } else {
            int initial = buf.position();
            byte[] plain = new byte[buf.remaining()];
            buf.get(plain);
            byte[] encrypted = session.getCipherOut().update(plain);
            buf.position(initial);
            buf.put(encrypted);
            buf.position(initial);
            nextFilter.filterWrite(ioSession, new DefaultWriteRequest(buf, writeRequest.getFuture()));
        }
    }

}
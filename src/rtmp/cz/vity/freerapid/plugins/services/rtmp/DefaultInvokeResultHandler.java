package cz.vity.freerapid.plugins.services.rtmp;

import java.util.logging.Logger;

/**
 * @author Peter Thomas
 */
public class DefaultInvokeResultHandler implements InvokeResultHandler {

    private static final Logger logger = Logger.getLogger(InvokeResultHandler.class.getName());

    public void handle(Invoke invoke, RtmpSession session) {
        String resultFor = session.getInvokedMethods().get(invoke.getSequenceId());
        logger.fine("result for method call: " + resultFor);
        if (resultFor.equals("connect")) {
            session.send(Packet.serverBw(0x001312d0)); // hard coded for now
            session.send(Packet.ping(3, 0, 300));
            session.send(new Invoke("createStream", 3));
        } else if (resultFor.equals("createStream")) {
            int streamId = invoke.getLastArgAsInt();
            logger.fine("value of streamId to play: " + streamId);
            Invoke play = new Invoke(streamId, "play", 8, null,
                    session.getPlayName(), session.getPlayStart(), session.getPlayDuration());
            session.send(play);
        } else {
            logger.warning("un-handled server result for: " + resultFor);
        }
    }

}
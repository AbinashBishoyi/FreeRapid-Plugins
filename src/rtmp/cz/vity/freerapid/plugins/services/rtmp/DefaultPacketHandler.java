package cz.vity.freerapid.plugins.services.rtmp;

import org.apache.mina.core.buffer.IoBuffer;

import java.util.List;
import java.util.logging.Logger;

/**
 * @author Peter Thomas
 * @author ntoskrnl
 */
class DefaultPacketHandler implements PacketHandler {
    private final static Logger logger = Logger.getLogger(DefaultPacketHandler.class.getName());
    private final static int BUFFER_TIME = 10 * 60 * 60 * 1000;//10 hours

    @Override
    public boolean handle(Packet packet, RtmpSession session) {
        IoBuffer data = packet.getData();
        switch (packet.getHeader().getPacketType()) {
            case CHUNK_SIZE:
                int newChunkSize = data.getInt();
                session.setChunkSize(newChunkSize);
                logger.fine("new chunk size is: " + newChunkSize);
                break;
            case CONTROL_MESSAGE:
                short type = data.getShort();
                if (type == 6) {
                    int time = data.getInt();
                    data.rewind();
                    logger.fine("server ping: " + packet);
                    Packet pong = Packet.ping(7, time, -1); // 7 == pong type
                    logger.fine("client pong: " + pong);
                    session.send(pong);
                } else if (type == 0x001A) {
                    logger.fine("server swf verification request: " + packet);
                    byte[] swfv = session.getSwfVerification();
                    if (swfv == null) {
                        logger.warning("not sending swf verification response! connect parameters not set, server likely to stop responding");
                    } else {
                        Packet pong = Packet.swfVerification(swfv);
                        logger.fine("sending client swf verification response: " + pong);
                        session.send(pong);
                    }
                } else if (type == 31) {
                    logger.fine("Server sent BufferEmpty, sending pause");
                    if (session.getPauseMode() == 0) {
                        final int time = session.getOutputWriter().getStatus().getVideoChannelTime();
                        session.setPauseTimestamp(time);
                        session.send(new Invoke("pause", 8, null, true, time));
                        session.setPauseMode(1);
                    } else if (session.getPauseMode() == 2) {
                        logger.fine("Sending unpause");
                        session.send(new Invoke("pause", 8, null, false, session.getPauseTimestamp()));
                        session.setPauseMode(3);
                    }
                } else if (type == 32) {
                    logger.fine("Server sent BufferReady");
                } else if (type == 1) {
                    logger.fine("Server sent Stream EOF");
                    if (session.getPauseMode() == 1) {
                        session.setPauseMode(2);
                    }
                } else {
                    logger.fine("not handling unknown control message type: " + type + " " + packet);
                }
                break;
            case AUDIO_DATA:
            case VIDEO_DATA:
                session.getOutputWriter().write(packet);
                break;
            case FLV_DATA:
                session.getOutputWriter().writeFlvData(data);
                break;
            case NOTIFY:
                AmfObject notify = new AmfObject();
                notify.decode(data, false);
                String notifyMethod = notify.getFirstPropertyAsString();
                logger.fine("server notify: " + notify);
                if (notifyMethod.equals("onMetaData")) {
                    logger.fine("notify is 'onMetadata', writing metadata");
                    data.rewind();
                    session.getOutputWriter().write(packet);
                    List<AmfProperty> properties = notify.getProperties();
                    if (properties != null && properties.size() >= 2) {
                        AmfProperty property = properties.get(1);
                        if (property != null) {
                            Object value = property.getValue();
                            if (value != null && value instanceof AmfObject) {
                                AmfProperty pDuration = ((AmfObject) value).getProperty("duration");
                                if (pDuration != null) {
                                    Object oDuration = pDuration.getValue();
                                    if (oDuration != null && oDuration instanceof Double) {
                                        double duration = (Double) oDuration;
                                        logger.fine("Stream duration: " + duration + " seconds");
                                        session.setStreamDuration((int) (duration * 1000));
                                    }
                                }
                            }
                        }
                    }
                }
                break;
            case INVOKE:
                Invoke invoke = new Invoke();
                invoke.decode(packet);
                String methodName = invoke.getMethodName();
                if (methodName.equals("_result")) {
                    String resultFor = session.getInvokedMethods().get(invoke.getSequenceId());
                    logger.fine("result for method call: " + resultFor);
                    if (resultFor.equals("connect")) {
                        if (session.getSecureToken() != null) {
                            AmfObject amf = invoke.getSecondArgAsAmfObject();
                            if (amf != null) {
                                AmfProperty amfprop = amf.getProperty("secureToken");
                                String secureToken = null;
                                if (amfprop != null) {
                                    secureToken = (String) amfprop.getValue();
                                }
                                if (secureToken != null) {
                                    logger.info("Sending secureToken challenge response");
                                    session.send(new Invoke("secureTokenResponse", 3, null,
                                            XXTEA.decrypt(secureToken, session.getSecureToken())));
                                }
                            }
                        }
                        session.send(Packet.serverBw(2500000)); // hard coded for now
                        session.send(new Invoke("createStream", 3));
                    } else if (resultFor.equals("createStream")) {
                        int streamId = invoke.getLastArgAsInt();
                        logger.fine("value of streamId to play: " + streamId);
                        Invoke play = new Invoke(streamId, "play", 8, null,
                                session.getPlayName(), session.getPlayStart(), session.getPlayDuration());
                        session.send(play);
                        session.send(Packet.ping(3, streamId, BUFFER_TIME));
                    } else if (resultFor.equals("secureTokenResponse")) {
                        logger.fine("server sent response for secureTokenResponse");
                    } else {
                        logger.warning("unhandled server result for: " + resultFor);
                    }
                } else if (methodName.equals("onStatus")) {
                    AmfObject temp = invoke.getSecondArgAsAmfObject();
                    String code = (String) temp.getProperty("code").getValue();
                    logger.fine("onStatus code: " + code);
                    if (code.equals("NetStream.Failed")
                            || code.equals("NetStream.Play.Failed")
                            || code.equals("NetStream.Play.Stop")
                            || code.equals("NetStream.Play.StreamNotFound")
                            || code.equals("NetConnection.Connect.InvalidApp")) {
                        logger.fine("disconnecting");
                        session.getDecoderOutput().disconnect();
                    } else if (code.equals("NetStream.Pause.Notify")) {
                        if (session.getPauseMode() == 1 || session.getPauseMode() == 2) {
                            logger.fine("Sending unpause");
                            session.send(new Invoke("pause", 8, null, false, session.getPauseTimestamp()));
                            session.setPauseMode(3);
                        }
                    }
                } else if (methodName.equals("onBWDone")) {
                    if (session.getBwCheckCounter() == 0) {
                        logger.fine("Server invoked onBWDone, invoking _checkbw");
                        Invoke checkbw = new Invoke("_checkbw", 3);
                        session.send(checkbw);
                        session.getInvokedMethods().remove(checkbw.getSequenceId());
                    } else {
                        logger.fine("Server invoked onBWDone, ignoring");
                    }
                } else if (methodName.equals("_onbwcheck")) {
                    logger.fine("Server invoked _onbwcheck, invoking _result");
                    int bwCheckCounter = session.getBwCheckCounter();
                    Header header = new Header(Header.Type.MEDIUM, 3, Packet.Type.INVOKE);
                    header.setTime(0x16 * bwCheckCounter);
                    IoBuffer body = AmfProperty.encode("_result", invoke.getSequenceId(), null, bwCheckCounter);
                    Packet result = new Packet(header, body);
                    logger.fine("Sending _onbwcheck result: " + result);
                    session.send(result);
                    session.setBwCheckCounter(bwCheckCounter + 1);
                } else if (methodName.equals("_onbwdone")) {
                    logger.fine("Server invoked _onbwdone");
                } else if (methodName.equals("_error")) {
                    logger.warning("Server sent error: " + invoke);
                } else if (methodName.equals("close")) {
                    logger.fine("Server requested close, disconnecting");
                    session.getDecoderOutput().disconnect();
                } else {
                    logger.fine("unhandled server invoke: " + invoke);
                }
                break;
            case BYTES_READ:
            case SERVER_BANDWIDTH:
            case CLIENT_BANDWIDTH:
                logger.fine("ignoring received packet: " + packet.getHeader());
                break;
            default:
                throw new RuntimeException("unknown packet type: " + packet.getHeader());
        }
        return true;
    }

}

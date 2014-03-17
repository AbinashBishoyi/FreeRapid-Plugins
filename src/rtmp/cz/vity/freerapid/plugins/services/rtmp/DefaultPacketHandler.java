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
                session.setInChunkSize(newChunkSize);
                logger.info("new chunk size is: " + newChunkSize);
                break;
            case CONTROL_MESSAGE:
                short type = data.getShort();
                if (type == 6) {
                    int time = data.getInt();
                    data.rewind();
                    logger.info("server ping: " + packet);
                    Packet pong = Packet.ping(7, time, -1); // 7 == pong type
                    logger.info("client pong: " + pong);
                    session.send(pong);
                } else if (type == 0x001A) {
                    logger.info("server swf verification request: " + packet);
                    byte swfvType = data.get(2);
                    if (swfvType > 1) {
                        logger.warning("swf verification type " + swfvType + " not supported, attempting to use type 1");
                    }
                    byte[] swfv = session.getSwfVerification();
                    if (swfv == null) {
                        logger.warning("swf verification parameters not set, not sending response");
                    } else {
                        Packet pong = Packet.swfVerification(swfv);
                        logger.info("sending client swf verification response: " + pong);
                        session.send(pong);
                    }
                } else if (type == 31) {
                    logger.info("Server sent BufferEmpty, sending pause");
                    if (session.getPauseMode() == PauseMode.PLAYING) {
                        final int time = session.getOutputWriter().getStatus().getVideoChannelTime();
                        session.setPauseTimestamp(time);
                        session.send(new Invoke("pause", 8, null, true, time));
                        session.setPauseMode(PauseMode.PAUSING);
                    } else if (session.getPauseMode() == PauseMode.PAUSED) {
                        logger.info("Sending unpause");
                        session.send(new Invoke("pause", 8, null, false, session.getPauseTimestamp()));
                        session.setPauseMode(PauseMode.RESUMING);
                    }
                } else if (type == 32) {
                    logger.info("Server sent BufferReady");
                } else if (type == 1) {
                    logger.info("Server sent Stream EOF");
                    if (session.getPauseMode() == PauseMode.PAUSING) {
                        session.setPauseMode(PauseMode.PAUSED);
                    }
                } else {
                    logger.info("not handling unknown control message type: " + type + " " + packet);
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
                logger.info("server notify: " + notify);
                if (notifyMethod.equals("onMetaData")) {
                    logger.info("notify is 'onMetadata', writing metadata");
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
                                        logger.info("Stream duration: " + duration + " seconds");
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
                    logger.info("result for method call: " + resultFor);
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
                        logger.info("value of streamId to play: " + streamId);
                        Invoke play = new Invoke(streamId, "play", 8, null, session.getPlayName(), 0);
                        session.send(play);
                        session.send(Packet.ping(3, streamId, BUFFER_TIME));
                    } else if (resultFor.equals("secureTokenResponse")) {
                        logger.info("server sent response for secureTokenResponse");
                    } else {
                        logger.warning("unhandled server result for: " + resultFor);
                    }
                } else if (methodName.equals("onStatus")) {
                    AmfObject temp = invoke.getSecondArgAsAmfObject();
                    String code = (String) temp.getProperty("code").getValue();
                    logger.info("onStatus code: " + code);
                    if (code.equals("NetStream.Failed")
                            || code.equals("NetStream.Play.Failed")
                            || code.equals("NetStream.Play.Stop")
                            || code.equals("NetStream.Play.StreamNotFound")
                            || code.equals("NetConnection.Connect.InvalidApp")) {
                        logger.info("disconnecting");
                        session.getDecoderOutput().disconnect();
                    } else if (code.equals("NetStream.Pause.Notify")) {
                        if (session.getPauseMode() == PauseMode.PAUSING || session.getPauseMode() == PauseMode.PAUSED) {
                            logger.info("Sending unpause");
                            session.send(new Invoke("pause", 8, null, false, session.getPauseTimestamp()));
                            session.setPauseMode(PauseMode.RESUMING);
                        }
                    }
                } else if (methodName.equals("onBWDone")) {
                    if (session.getBwCheckCounter() == 0) {
                        logger.info("Server invoked onBWDone, invoking _checkbw");
                        Invoke checkbw = new Invoke("_checkbw", 3);
                        session.send(checkbw);
                        session.getInvokedMethods().remove(checkbw.getSequenceId());
                    } else {
                        logger.info("Server invoked onBWDone, ignoring");
                    }
                } else if (methodName.equals("_onbwcheck")) {
                    logger.info("Server invoked _onbwcheck, invoking _result");
                    int bwCheckCounter = session.getBwCheckCounter();
                    Header header = new Header(Header.Type.MEDIUM, 3, Packet.Type.INVOKE);
                    header.setTime(0x16 * bwCheckCounter);
                    IoBuffer body = AmfProperty.encode("_result", invoke.getSequenceId(), null, bwCheckCounter);
                    Packet result = new Packet(header, body);
                    logger.info("Sending _onbwcheck result: " + result);
                    session.send(result);
                    session.setBwCheckCounter(bwCheckCounter + 1);
                } else if (methodName.equals("_onbwdone")) {
                    logger.info("Server invoked _onbwdone");
                } else if (methodName.equals("_error")) {
                    logger.warning("Server sent error: " + invoke);
                } else if (methodName.equals("close")) {
                    logger.info("Server requested close, disconnecting");
                    session.getDecoderOutput().disconnect();
                } else {
                    logger.info("unhandled server invoke: " + invoke);
                }
                break;
            case BYTES_READ:
            case SERVER_BANDWIDTH:
            case CLIENT_BANDWIDTH:
                logger.info("ignoring received packet: " + packet.getHeader());
                break;
            default:
                throw new RuntimeException("unknown packet type: " + packet.getHeader());
        }
        return true;
    }

}

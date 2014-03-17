package cz.vity.freerapid.plugins.services.yleareena;

import cz.vity.freerapid.plugins.services.rtmp.*;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.mina.core.buffer.IoBuffer;

import java.util.logging.Logger;

/**
 * @author ntoskrnl
 */
class YleAreenaPacketHandler implements PacketHandler {
    private final static Logger logger = Logger.getLogger(YleAreenaPacketHandler.class.getName());

    private final String id;

    public YleAreenaPacketHandler(final String id) {
        this.id = id;
    }

    @Override
    public boolean handle(final Packet packet, final RtmpSession session) {
        if (packet.getHeader().getPacketType() == Packet.Type.INVOKE) {
            final Invoke invoke = new Invoke();
            invoke.decode(packet);
            final String methodName = invoke.getMethodName();
            if (methodName.equals("_result")) {
                if ("connect".equals(session.getInvokedMethods().get(invoke.getSequenceId()))) {
                    logger.info("Result for connect received");
                    session.send(Packet.serverBw(0x001312d0));
                    return false;
                }
            } else if (methodName.equals("authenticationDetails")) {
                logger.info("authenticationDetails received");
                final AmfObject temp = invoke.getSecondArgAsAmfObject();
                final boolean locatedInBroadcastTerritory = (Boolean) temp.getProperty("locatedInBroadcastTerritory").getValue();
                logger.info("locatedInBroadcastTerritory: " + locatedInBroadcastTerritory);
                final long randomAuth = ((Number) temp.getProperty("randomAuth").getValue()).longValue();
                logger.info("randomAuth: " + randomAuth);
                final long authResult = (randomAuth + 447537687) % 6834253;
                logger.info("authResult: " + authResult);
                final boolean tvFeeActivated = (Boolean) temp.getProperty("tvFeeActivated").getValue();
                logger.info("tvFeeActivated: " + tvFeeActivated);
                final Header h = new Header(Header.Type.LARGE, 3, Packet.Type.FLEX_MESSAGE);
                final IoBuffer b = IoBuffer.allocate(47);
                b.put((byte) 0);
                AmfProperty.encode(b, "authenticateRandomNumber", 0, null, authResult);
                session.send(new Packet(h, b));
                return false;
            } else if (methodName.equals("randomNumberAuthenticated")) {
                final Header h = new Header(Header.Type.LARGE, 3, Packet.Type.FLEX_MESSAGE);
                final IoBuffer b = IoBuffer.allocate(128);
                b.put((byte) 0);
                AmfProperty.encode(b, "requestData", 0, null, "e1", "clips/info/" + id);
                session.send(new Packet(h, b));
                return false;
            } else if (methodName.equals("rpcResult")) {
                try {
                    final String json = (String) invoke.getArgs()[2];
                    logger.info("json = " + json);
                    final String url = PlugUtils.getStringBetween(json, "\"url\":\"", "\"").replace("\\", "");
                    logger.info("url = " + url);
                    String playName = url.substring(34);
                    if (playName.endsWith(".mp4") && !playName.startsWith("mp4:")) {
                        playName = "mp4:" + playName;
                    } else if (playName.endsWith(".mp3")) {
                        playName = playName.substring(0, playName.length() - 4);
                        if (!playName.startsWith("mp3:")) {
                            playName = "mp3:" + playName;
                        }
                    }
                    logger.info("playName = " + playName);
                    session.setPlayName(playName);
                    session.send(new Invoke("createStream", 3));
                    return false;
                } catch (Exception e) {
                    throw new RuntimeException("Error parsing JSON response", e);
                }
            }
        }
        return true;
    }

}

package cz.vity.freerapid.plugins.services.rtmp;

import org.apache.mina.core.buffer.IoBuffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static cz.vity.freerapid.plugins.services.rtmp.Header.Type.LARGE;
import static cz.vity.freerapid.plugins.services.rtmp.Header.Type.MEDIUM;

/**
 * @author Peter Thomas
 */
public class Invoke {

    private static final Logger logger = Logger.getLogger(Invoke.class.getName());

    private String methodName;
    private long sequenceId;
    private int channelId;
    private int time;
    private int streamId = -1;
    private Object[] args;

    public Invoke() {
    }

    public Invoke(String methodName, int channelId, Object... args) {
        this.methodName = methodName;
        this.channelId = channelId;
        this.args = args;
    }

    public Invoke(int streamId, String methodName, int channelId, Object... args) {
        this(methodName, channelId, args);
        this.streamId = streamId;
    }

    public Object[] getArgs() {
        return args;
    }

    public int getLastArgAsInt() {
        return new Double(args[args.length - 1].toString()).intValue();
    }

    public AmfObject getSecondArgAsAmfObject() { // TODO significance of first ?
        return (AmfObject) args[1];
    }

    public void setTime(int time) {
        this.time = time;
    }

    public long getSequenceId() {
        return sequenceId;
    }

    public String getMethodName() {
        return methodName;
    }

    public Packet encode(RtmpSession session) {
        sequenceId = session.getNextInvokeId();
        session.getInvokedMethods().put(sequenceId, methodName);
        Header prevHeader = session.getPrevHeadersOut().get(channelId);
        Header.Type headerType = prevHeader == null ? LARGE : MEDIUM;
        Header header = new Header(headerType, channelId, Packet.Type.INVOKE);
        if (streamId != -1) {
            header.setStreamId(streamId);
        }
        List<Object> list = new ArrayList<Object>();
        list.add(methodName);
        list.add(sequenceId);
        if (args != null && args.length > 0) {
            for (Object arg : args) {
                if (arg instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) arg;
                    list.add(new AmfObject(map));
                } else {
                    list.add(arg);
                }
            }
        } else {
            list.add(null);
        }
        header.setTime(time);
        IoBuffer body = AmfProperty.encode(list.toArray());
        Packet packet = new Packet(header, body);
        session.getPrevHeadersOut().put(channelId, header);
        logger.info("encoded invoke: " + toString());
        return packet;
    }

    public void decode(Packet packet) {
        channelId = packet.getHeader().getChannelId();
        streamId = packet.getHeader().getStreamId();
        AmfObject object = new AmfObject();
        object.decode(packet.getData(), false);
        List<AmfProperty> properties = object.getProperties();
        methodName = (String) properties.get(0).getValue();
        Double temp = (Double) properties.get(1).getValue();
        sequenceId = temp.longValue();
        if (properties.size() > 2) {
            int argsLength = properties.size() - 2;
            args = new Object[argsLength];
            for (int i = 0; i < argsLength; i++) {
                args[i] = properties.get(i + 2).getValue();
            }
        }
        logger.info("decoded invoke: " + toString());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[method: ").append(methodName);
        sb.append(", sequenceId: ").append(sequenceId);
        if (streamId != -1) {
            sb.append(", streamId: ").append(streamId);
        }
        sb.append(", args: ").append(Arrays.toString(args)).append(']');
        return sb.toString();
    }

}
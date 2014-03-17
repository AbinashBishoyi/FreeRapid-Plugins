package cz.vity.freerapid.plugins.services.rtmp;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.apache.mina.core.session.IoSession;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Peter Thomas
 * @author ntoskrnl
 */
public class RtmpSession {

    private static final Logger logger = Logger.getLogger(RtmpSession.class.getName());

    private static final String RTMP_SESSION_KEY = "RTMP_SESSION_KEY";

    private boolean serverHandshakeReceived;
    private boolean handshakeComplete;
    private Map<Integer, Header> prevHeadersIn = new ConcurrentHashMap<Integer, Header>();
    private Map<Integer, Header> prevHeadersOut = new ConcurrentHashMap<Integer, Header>();
    private Map<Integer, Packet> prevPacketsIn = new ConcurrentHashMap<Integer, Packet>();
    private Map<Long, String> invokedMethods = new ConcurrentHashMap<Long, String>();
    private int chunkSize = 128;
    private long nextInvokeId;
    private long bytesReadLastSent;
    private Map<String, Object> connectParams;
    private String playName;
    private int playStart;
    private int playDuration = -2;
    private int streamDuration;
    private OutputWriter outputWriter;
    private DecoderOutput decoderOutput;
    private String host;
    private int port;
    private LinkedList<PacketHandler> packetHandlers;
    private boolean encrypted;
    private HandshakeType handshakeType;
    private KeyAgreement keyAgreement;
    private byte[] clientPublicKey;
    private Cipher cipherIn;
    private Cipher cipherOut;
    private int swfSize;
    private byte[] swfHash;
    private byte[] swfVerification;
    private byte[] clientDigest;
    private byte[] serverDigest;
    private byte[] serverResponse;
    private int pauseTimestamp;
    private int pauseMode = 0;
    private HttpFile httpFile;
    private ConnectionSettings connectionSettings;
    private String secureToken;
    private int bwCheckCounter;

    /**
     * Empty constructor. Mainly for internal use.
     */
    public RtmpSession() {
    }

    /**
     * Constructs a new RtmpSession with the specified parameters.
     *
     * @param host     server
     * @param port     port
     * @param app      app
     * @param playName play name
     */
    public RtmpSession(String host, int port, String app, String playName) {
        initConnectParams(host, port, app, playName, false);
    }

    /**
     * Constructs a new RtmpSession with the specified parameters.
     *
     * @param host      server
     * @param port      port
     * @param app       app
     * @param playName  play name
     * @param encrypted true if RTMPE should be used, plain RTMP otherwise
     */
    public RtmpSession(String host, int port, String app, String playName, boolean encrypted) {
        initConnectParams(host, port, app, playName, encrypted);
    }

    /**
     * Constructs a new RtmpSession with the specified parameters.
     *
     * @param host     server
     * @param port     port
     * @param app      app
     * @param playName play name
     * @param protocol protocol
     */
    public RtmpSession(String host, int port, String app, String playName, String protocol) {
        initConnectParams(host, port, app, playName, protocol);
    }

    /**
     * Constructs a new RtmpSession with the specified play name and other parameters parsed from a URL.
     *
     * @param url      URL to parse
     * @param playName play name
     * @throws PluginImplementationException if URL is invalid
     */
    public RtmpSession(String url, String playName) throws PluginImplementationException {
        Pattern pattern = Pattern.compile("(rtmp(?:e|t|s|te|ts)?)://([^/:]+)(:[0-9]+)?/(.+)");
        Matcher matcher = pattern.matcher(url);
        if (!matcher.matches()) {
            throw new PluginImplementationException("Invalid RTMP url: " + url);
        }
        logger.fine("parsing url: " + url);
        String protocol = matcher.group(1);
        logger.fine("protocol = '" + protocol + "'");
        String hostString = matcher.group(2);
        logger.fine("host = '" + hostString + "'");
        String portString = matcher.group(3);
        if (portString == null) {
            logger.fine("port is null in url, will use default 1935");
        } else {
            portString = portString.substring(1); // skip the ':'
            logger.fine("port = '" + portString + "'");
        }
        String appString = matcher.group(4);
        logger.fine("app = '" + appString + "'");
        logger.fine("play = '" + playName + "'");
        int portInt = portString == null ? 1935 : Integer.parseInt(portString);
        initConnectParams(hostString, portInt, appString, playName, protocol);
    }

    /**
     * Constructs a new RtmpSession with parameters parsed from a URL.
     *
     * @param url URL to parse
     * @throws PluginImplementationException if URL is invalid
     */
    public RtmpSession(String url) throws PluginImplementationException {
        Pattern pattern = Pattern.compile("(rtmp(?:e|t|s|te|ts)?)://([^/:]+)(:[0-9]+)?/([^/]+)/(.*)");
        Matcher matcher = pattern.matcher(url);
        if (!matcher.matches()) {
            throw new PluginImplementationException("Invalid RTMP url: " + url);
        }
        logger.fine("parsing url: " + url);
        String protocol = matcher.group(1);
        logger.fine("protocol = '" + protocol + "'");
        String hostString = matcher.group(2);
        logger.fine("host = '" + hostString + "'");
        String portString = matcher.group(3);
        if (portString == null) {
            logger.fine("port is null in url, will use default 1935");
        } else {
            portString = portString.substring(1); // skip the ':'
            logger.fine("port = '" + portString + "'");
        }
        String appString = matcher.group(4);
        logger.fine("app = '" + appString + "'");
        String playString = matcher.group(5);
        logger.fine("play = '" + playString + "'");
        int portInt = portString == null ? 1935 : Integer.parseInt(portString);
        initConnectParams(hostString, portInt, appString, playString, protocol);
    }

    private void initConnectParams(String host, int port, String app, String playName, String protocol) {
        initConnectParams(host, port, app, playName, isProtocolEncrypted(protocol));
    }

    private void initConnectParams(String host, int port, String app, String playName, boolean encrypted) {
        this.host = host;
        this.port = port;
        this.playName = playName;
        this.encrypted = encrypted;
        String tcUrl = (encrypted ? "rtmpe://" : "rtmp://") + host + ":" + port + "/" + app;
        connectParams = new HashMap<String, Object>();
        connectParams.put("objectEncoding", 0);
        connectParams.put("app", app);
        connectParams.put("flashVer", "WIN 10,1,53,64");
        connectParams.put("fpad", false);
        connectParams.put("tcUrl", tcUrl);
        connectParams.put("audioCodecs", 3191);
        connectParams.put("videoFunction", 1);
        connectParams.put("capabilities", 239);
        connectParams.put("videoCodecs", 252);
        outputWriter = new FlvStreamWriter(playStart, this);
        packetHandlers = new LinkedList<PacketHandler>();
        addPacketHandler(new DefaultPacketHandler());
    }

    public static boolean isProtocolEncrypted(String protocol) throws NullPointerException {
        return protocol.equalsIgnoreCase("rtmpe") || protocol.equalsIgnoreCase("rtmpte");
    }

    public static RtmpSession getFrom(IoSession ioSession) {
        return (RtmpSession) ioSession.getAttribute(RTMP_SESSION_KEY);
    }

    public void putInto(IoSession ioSession) {
        ioSession.setAttribute(RTMP_SESSION_KEY, this);
    }

    public void send(Handshake handshake) {
        decoderOutput.write(handshake);
    }

    public void send(Packet packet) {
        decoderOutput.write(packet);
    }

    public void send(Invoke invoke) {
        send(invoke.encode(this));
    }

    public String resultFor(Invoke invoke) {
        return getInvokedMethods().get(invoke.getSequenceId());
    }

    public long getNextInvokeId() {
        return ++nextInvokeId;
    }

    public void setSwfHash(String swfHash) {
        this.swfHash = Utils.fromHex(swfHash);
    }

    public void initSwfVerification(String pathToLocalSwfFile) {
        initSwfVerification(new File(pathToLocalSwfFile));
    }

    public void initSwfVerification(File localSwfFile) {
        logger.info("initializing swf verification data for: " + localSwfFile.getAbsolutePath());
        try {
            swfHash = new byte[32];
            swfSize = SwfVerificationHelper.getSwfHash(localSwfFile, swfHash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        logger.info("swf hash: " + Utils.toHex(swfHash, false));
        logger.info("swf size: " + swfSize);
    }

    //==========================================================================

    public byte[] getServerResponse() {
        return serverResponse;
    }

    public void setServerResponse(byte[] serverResponse) {
        this.serverResponse = serverResponse;
    }

    public boolean isHandshakeComplete() {
        return handshakeComplete;
    }

    public void setHandshakeComplete(boolean handshakeComplete) {
        this.handshakeComplete = handshakeComplete;
    }

    public byte[] getServerDigest() {
        return serverDigest;
    }

    public void setServerDigest(byte[] serverDigest) {
        this.serverDigest = serverDigest;
    }

    public byte[] getClientDigest() {
        return clientDigest;
    }

    public void setClientDigest(byte[] clientDigest) {
        this.clientDigest = clientDigest;
    }

    public byte[] getSwfVerification() {
        return swfVerification;
    }

    public void setSwfVerification(byte[] swfVerification) {
        this.swfVerification = swfVerification;
    }

    public int getSwfSize() {
        return swfSize;
    }

    public void setSwfSize(int swfSize) {
        this.swfSize = swfSize;
    }

    public byte[] getSwfHash() {
        return swfHash;
    }

    public void setSwfHash(byte[] swfHash) {
        this.swfHash = swfHash;
    }

    public Cipher getCipherIn() {
        return cipherIn;
    }

    public void setCipherIn(Cipher cipherIn) {
        this.cipherIn = cipherIn;
    }

    public Cipher getCipherOut() {
        return cipherOut;
    }

    public void setCipherOut(Cipher cipherOut) {
        this.cipherOut = cipherOut;
    }

    public byte[] getClientPublicKey() {
        return clientPublicKey;
    }

    public void setClientPublicKey(byte[] clientPublicKey) {
        this.clientPublicKey = clientPublicKey;
    }

    public KeyAgreement getKeyAgreement() {
        return keyAgreement;
    }

    public void setKeyAgreement(KeyAgreement keyAgreement) {
        this.keyAgreement = keyAgreement;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public HandshakeType getHandshakeType() {
        return handshakeType;
    }

    public void setHandshakeType(final HandshakeType handshakeType) {
        this.handshakeType = handshakeType;
    }

    public List<PacketHandler> getPacketHandlers() {
        return packetHandlers;
    }

    /**
     * Adds a PacketHandler as first in the list
     *
     * @param handler handler to add
     */
    public void addPacketHandler(PacketHandler handler) {
        packetHandlers.addFirst(handler);
    }

    /**
     * Removes the PacketHandler that is first in the list
     */
    public void removePacketHandler() {
        if (!packetHandlers.isEmpty()) {
            packetHandlers.removeFirst();
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getPlayStart() {
        return playStart;
    }

    public void setPlayStart(int playStart) {
        this.playStart = playStart;
    }

    public DecoderOutput getDecoderOutput() {
        return decoderOutput;
    }

    public void setDecoderOutput(DecoderOutput decoderOutput) {
        this.decoderOutput = decoderOutput;
    }

    public OutputWriter getOutputWriter() {
        return outputWriter;
    }

    public void setOutputWriter(OutputWriter outputWriter) {
        this.outputWriter = outputWriter;
    }

    public int getPlayDuration() {
        return playDuration;
    }

    public void setPlayDuration(int playDuration) {
        this.playDuration = playDuration;
    }

    public String getPlayName() {
        return playName;
    }

    public void setPlayName(String playName) {
        this.playName = playName;
    }

    public Map<String, Object> getConnectParams() {
        return connectParams;
    }

    public void setConnectParams(Map<String, Object> connectParams) {
        this.connectParams = connectParams;
    }

    public long getBytesReadLastSent() {
        return bytesReadLastSent;
    }

    public void setBytesReadLastSent(long bytesReadLastSent) {
        this.bytesReadLastSent = bytesReadLastSent;
    }

    public Map<Long, String> getInvokedMethods() {
        return invokedMethods;
    }

    public boolean isServerHandshakeReceived() {
        return serverHandshakeReceived;
    }

    public void setServerHandshakeReceived(boolean serverHandshakeReceived) {
        this.serverHandshakeReceived = serverHandshakeReceived;
    }

    public Map<Integer, Header> getPrevHeadersIn() {
        return prevHeadersIn;
    }

    public Map<Integer, Header> getPrevHeadersOut() {
        return prevHeadersOut;
    }

    public Map<Integer, Packet> getPrevPacketsIn() {
        return prevPacketsIn;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getStreamDuration() {
        return streamDuration;
    }

    public void setStreamDuration(int streamDuration) {
        this.streamDuration = streamDuration;
    }

    public int getPauseTimestamp() {
        return pauseTimestamp;
    }

    public void setPauseTimestamp(int pauseTimestamp) {
        this.pauseTimestamp = pauseTimestamp;
    }

    public int getPauseMode() {
        return pauseMode;
    }

    public void setPauseMode(int pauseMode) {
        this.pauseMode = pauseMode;
    }

    public HttpFile getHttpFile() {
        return httpFile;
    }

    public void setHttpFile(HttpFile httpFile) {
        this.httpFile = httpFile;
    }

    public ConnectionSettings getConnectionSettings() {
        return connectionSettings;
    }

    public void setConnectionSettings(ConnectionSettings connectionSettings) {
        this.connectionSettings = connectionSettings;
    }

    public String getSecureToken() {
        return secureToken;
    }

    public void setSecureToken(String secureToken) {
        this.secureToken = secureToken;
    }

    public int getBwCheckCounter() {
        return bwCheckCounter;
    }

    public void setBwCheckCounter(int bwCheckCounter) {
        this.bwCheckCounter = bwCheckCounter;
    }

}
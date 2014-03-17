package cz.vity.freerapid.plugins.services.rtmp;

import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.proxy.AbstractProxyIoHandler;
import org.apache.mina.proxy.ProxyConnector;
import org.apache.mina.proxy.handlers.socks.SocksProxyConstants;
import org.apache.mina.proxy.handlers.socks.SocksProxyRequest;
import org.apache.mina.proxy.session.ProxyIoSession;
import org.apache.mina.transport.socket.SocketConnector;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.logging.Logger;

/**
 * @author Peter Thomas
 * @author ntoskrnl
 */
public class RtmpClient extends IoHandlerAdapter {

    private static final Logger logger = Logger.getLogger(RtmpClient.class.getName());

    private final RtmpSession session;
    private final IoConnector connector;
    private IoSession ioSession;

    private boolean closed = false;

    public RtmpClient(RtmpSession session) {
        this.session = session;
        SocketConnector socketConnector = new NioSocketConnector();
        socketConnector.getFilterChain().addLast("crypto", new RtmpeIoFilter());
        socketConnector.getFilterChain().addLast("codec", new ProtocolCodecFilter(new RtmpCodecFactory()));
        ConnectionSettings settings = session.getConnectionSettings();
        if (settings != null && settings.isProxySet() && settings.getProxyType() == Proxy.Type.SOCKS) {
            this.connector = getProxyConnector(socketConnector, settings);
        } else {
            socketConnector.setDefaultRemoteAddress(new InetSocketAddress(session.getHost(), session.getPort()));
            socketConnector.setHandler(this);
            this.connector = socketConnector;
        }
    }

    private ProxyConnector getProxyConnector(SocketConnector socketConnector, ConnectionSettings settings) {
        ProxyConnector connector = new ProxyConnector(socketConnector);
        connector.setHandler(new ProxyIoHandler(this));
        SocksProxyRequest proxyRequest = new SocksProxyRequest(
                SocksProxyConstants.SOCKS_VERSION_5,
                SocksProxyConstants.ESTABLISH_TCPIP_STREAM,
                new InetSocketAddress(session.getHost(), session.getPort()),
                settings.getUserName() /* null is ok */
        );
        proxyRequest.setPassword(settings.getPassword() /* null is ok */);
        connector.setProxyIoSession(
                new ProxyIoSession(
                        new InetSocketAddress(settings.getProxyURL(), settings.getProxyPort()),
                        proxyRequest
                )
        );
        return connector;
    }

    public void connect() {
        connector.connect();
    }

    @Override
    public void sessionOpened(IoSession ioSession) throws Exception {
        this.ioSession = ioSession;
        session.setDecoderOutput(new MinaIoSessionOutput(this));
        session.putInto(ioSession);
        logger.info("session opened, starting handshake");
        ioSession.write(Handshake.generateClientRequest1(session));
    }

    @Override
    public void exceptionCaught(IoSession ioSession, Throwable cause) throws Exception {
        LogUtils.processException(logger, cause);
        disconnect();
    }

    public synchronized void disconnect() {
        if (!closed) {
            closed = true;
            // A new thread is needed to prevent deadlocks
            // in case this method is called from a worked thread.
            // (It would wait for itself to complete otherwise.)
            new Thread(new Runnable() {
                public void run() {
                    try {
                        if (ioSession != null) {
                            logger.info("disconnecting, bytes read: " + ioSession.getReadBytes());
                            CloseFuture future = ioSession.close(true);
                            logger.info("closing connection, waiting for thread exit");
                            future.awaitUninterruptibly();
                            logger.info("connection closed successfully");
                        }
                        logger.fine("disposing connector");
                        connector.dispose();
                        logger.fine("connector disposed successfully");
                    } finally {
                        session.getOutputWriter().close();
                    }
                }
            }, "disconnect").start();
        }
    }

    private static class ProxyIoHandler extends AbstractProxyIoHandler {

        private IoHandler ioHandler;

        public ProxyIoHandler(IoHandler ioHandler) {
            this.ioHandler = ioHandler;
        }

        public void proxySessionOpened(IoSession ioSession) throws Exception {
            ioHandler.sessionOpened(ioSession);
        }
    }

    private static class RtmpCodecFactory implements ProtocolCodecFactory {

        private ProtocolEncoder encoder = new RtmpEncoder();
        private ProtocolDecoder decoder = new RtmpDecoder();

        public ProtocolDecoder getDecoder(IoSession ioSession) {
            return decoder;
        }

        public ProtocolEncoder getEncoder(IoSession ioSession) {
            return encoder;
        }
    }

    /**
     * implementation used for connecting to a network stream
     */
    private static class MinaIoSessionOutput implements DecoderOutput {

        private RtmpClient client;

        public MinaIoSessionOutput(RtmpClient client) {
            this.client = client;
        }

        public void write(Object packet) {
            client.ioSession.write(packet);
        }

        public void disconnect() {
            client.disconnect();
        }
    }
}
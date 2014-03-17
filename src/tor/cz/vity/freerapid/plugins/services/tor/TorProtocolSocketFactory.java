package cz.vity.freerapid.plugins.services.tor;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;

import javax.net.SocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * @author ntoskrnl
 */
public class TorProtocolSocketFactory implements ProtocolSocketFactory {

    private final SocketFactory socketFactory;

    public TorProtocolSocketFactory(final SocketFactory socketFactory) {
        this.socketFactory = socketFactory;
    }

    @Override
    public Socket createSocket(final String host, final int port, final InetAddress localAddress, final int localPort) throws IOException, UnknownHostException {
        return createSocket(host, port);
    }

    @Override
    public Socket createSocket(final String host, final int port, final InetAddress localAddress, final int localPort, final HttpConnectionParams params) throws IOException, UnknownHostException, ConnectTimeoutException {
        return createSocket(host, port);
    }

    @Override
    public Socket createSocket(final String host, final int port) throws IOException, UnknownHostException {
        return socketFactory.createSocket(host, port);
    }

}

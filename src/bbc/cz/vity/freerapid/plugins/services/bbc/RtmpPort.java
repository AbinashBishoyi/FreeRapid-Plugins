package cz.vity.freerapid.plugins.services.bbc;

/**
 * @author tong2shot
 */
enum RtmpPort {
    _1935(1935),
    _80(80),
    _443(443);

    private final int port;

    RtmpPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return port + (port == 1935 ? " (default)" : "");
    }
}

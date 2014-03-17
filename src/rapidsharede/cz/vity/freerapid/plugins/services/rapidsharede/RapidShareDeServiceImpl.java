package cz.vity.freerapid.plugins.services.rapidsharede;

import cz.vity.freerapid.plugins.services.rapidsharede.ssl.EasySSLProtocolSocketFactory;
import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;

/**
 * @author Kajda
 */
public class RapidShareDeServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "rapidshare.de";

    public RapidShareDeServiceImpl() {
        super();
        try {
            trustAllCerts();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new RapidShareDeFileRunner();
    }

    private static void trustAllCerts() throws Exception {
        ProtocolSocketFactory sf = new EasySSLProtocolSocketFactory();
        Protocol p = new Protocol("https", sf, 443);
        Protocol.registerProtocol("https", p);
    }
}
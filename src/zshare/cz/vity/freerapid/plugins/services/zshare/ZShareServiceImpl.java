package cz.vity.freerapid.plugins.services.zshare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;


/**
 * @author Prathap Reddy
 */
public class ZShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "zshare.net";
    
    //zshare allows unlimited downloads simaltaniously
    private static final int MAX_DOWNLOADS_FROM_THIS_IP = 1000;//think this is as unlimited :-)

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return MAX_DOWNLOADS_FROM_THIS_IP;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ZShareRunner();
    }
}
package cz.vity.freerapid.plugins.services.tinyurl;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Alex
 */
public class TinyUrlServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "tinyUrl.com";

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 10;
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new TinyUrlRunner();
    }

}

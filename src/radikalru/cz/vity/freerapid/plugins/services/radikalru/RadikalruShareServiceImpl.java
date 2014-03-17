package cz.vity.freerapid.plugins.services.radikalru;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Alex
 */
public class RadikalruShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "radikal.ru";

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 3;
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new RadikalruRunner();
    }

}

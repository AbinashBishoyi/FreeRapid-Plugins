package cz.vity.freerapid.plugins.services.forshared;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Alex
 */
public class ForSharedServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "4shared.com";

    @Override
    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public int getMaxDownloadsFromOneIP() {
        return 9;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ForSharedRunner();
    }

}

package cz.vity.freerapid.plugins.services.rapidsharede;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Kajda
 */
public class RapidShareDeServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "rapidshare.de";

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 9;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new RapidShareDeFileRunner();
    }

}

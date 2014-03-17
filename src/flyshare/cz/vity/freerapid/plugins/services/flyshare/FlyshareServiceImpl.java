package cz.vity.freerapid.plugins.services.flyshare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Ladislav Vitasek
 */
public class FlyshareServiceImpl extends AbstractFileShareService {

    private static final String SERVICE_NAME = "flyshare.cz";

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 2;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FlyshareRunner();
    }


}

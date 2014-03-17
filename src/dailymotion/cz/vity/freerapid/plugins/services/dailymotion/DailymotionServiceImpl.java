package cz.vity.freerapid.plugins.services.dailymotion;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 */
public class DailymotionServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "dailymotion.com";

    @Override
    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public int getMaxDownloadsFromOneIP() {
        return 8;//i checked that before
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new DailymotionRunner();
    }

}

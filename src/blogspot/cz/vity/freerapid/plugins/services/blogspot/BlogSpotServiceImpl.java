package cz.vity.freerapid.plugins.services.blogspot;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Alex
 */
public class BlogSpotServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "blogspot.com";

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
        return new BlogSpotRunner();
    }

}

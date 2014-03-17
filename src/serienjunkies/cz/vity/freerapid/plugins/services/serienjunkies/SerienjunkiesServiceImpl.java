package cz.vity.freerapid.plugins.services.serienjunkies;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author benpicco
 */
public class SerienjunkiesServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "serienjunkies.org";
    }

    public int getMaxDownloadsFromOneIP() {
        return 10; //TODO: check
    }

    @Override
    public boolean supportsRunCheck() {
        return false;//Check not supported
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SerienjunkiesFileRunner();
    }

}

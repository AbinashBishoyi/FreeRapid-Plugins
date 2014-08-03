package cz.vity.freerapid.plugins.services.tvrelease;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class TvReleaseServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "tv-release.net";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new TvReleaseFileRunner();
    }

}
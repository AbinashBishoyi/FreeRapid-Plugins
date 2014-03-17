package cz.vity.freerapid.plugins.services.applemovies;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class AppleMoviesServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "movies.apple.com";
    }

    public int getMaxDownloadsFromOneIP() {
        return 9;
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new AppleMoviesFileRunner();
    }

}
package cz.vity.freerapid.plugins.services.movielibrary;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author Vity
 */
public class MovieLibraryServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "movielibrary.cz";
    }

    @Override
    public int getMaxDownloadsFromOneIP() {
        return 10;
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MovieLibraryFileRunner();
    }

}
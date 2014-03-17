package cz.vity.freerapid.plugins.services.flickrcollections;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author Arthur
 */
public class flickrCollectionsServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "flickrcollections.com";
    }

    public int getMaxDownloadsFromOneIP() {
        //TODO don't forget to update this value, in plugin.xml don't forget to update this value too
        return 1;
    }

    @Override
    public boolean supportsRunCheck() {
        return false;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new flickrCollectionsFileRunner();
    }

}
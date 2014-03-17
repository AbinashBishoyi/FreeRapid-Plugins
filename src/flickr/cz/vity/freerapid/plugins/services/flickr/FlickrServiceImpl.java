package cz.vity.freerapid.plugins.services.flickr;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author Arthur
 */
public class FlickrServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "flickr.com";
    }

    public int getMaxDownloadsFromOneIP() {
        //TODO don't forget to update this value, in plugin.xml don't forget to update this value too
        return 5;
    }

    @Override
    public boolean supportsRunCheck() {
        return false;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FlickrFileRunner();
    }

}
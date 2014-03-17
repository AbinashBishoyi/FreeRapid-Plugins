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

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FlickrFileRunner();
    }

}
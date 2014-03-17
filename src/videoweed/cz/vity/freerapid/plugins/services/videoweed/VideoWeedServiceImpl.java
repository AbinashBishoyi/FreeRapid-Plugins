package cz.vity.freerapid.plugins.services.videoweed;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author TommyTom
 */
public class VideoWeedServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "videoweed.com";
    }

    public int getMaxDownloadsFromOneIP() {
        return 9;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new VideoWeedFileRunner();
    }

}
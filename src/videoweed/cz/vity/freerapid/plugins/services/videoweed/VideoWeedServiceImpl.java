package cz.vity.freerapid.plugins.services.videoweed;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author TommyTom
 */
public class VideoWeedServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "videoweed.es";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new VideoWeedFileRunner();
    }

}
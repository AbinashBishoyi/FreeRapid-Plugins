package cz.vity.freerapid.plugins.services.videomega;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class VideoMegaServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "videomega.tv";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new VideoMegaFileRunner();
    }

}
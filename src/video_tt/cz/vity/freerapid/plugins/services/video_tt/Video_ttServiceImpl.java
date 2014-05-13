package cz.vity.freerapid.plugins.services.video_tt;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class Video_ttServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "video.tt";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new Video_ttFileRunner();
    }

}
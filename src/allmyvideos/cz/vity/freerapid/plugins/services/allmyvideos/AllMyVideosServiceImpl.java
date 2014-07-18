package cz.vity.freerapid.plugins.services.allmyvideos;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class AllMyVideosServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getServiceTitle() {
        return "AllMyVideos";
    }

    @Override
    public String getName() {
        return "allmyvideos.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new AllMyVideosFileRunner();
    }

}
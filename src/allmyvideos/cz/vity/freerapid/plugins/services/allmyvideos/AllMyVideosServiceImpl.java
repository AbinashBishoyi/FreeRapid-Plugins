package cz.vity.freerapid.plugins.services.allmyvideos;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class AllMyVideosServiceImpl extends XFileSharingServiceImpl {

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
package cz.vity.freerapid.plugins.services.cloudyvideos;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class CloudyVideosServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getServiceTitle() {
        return "CloudyVideos";
    }

    @Override
    public String getName() {
        return "cloudyvideos.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new CloudyVideosFileRunner();
    }

}
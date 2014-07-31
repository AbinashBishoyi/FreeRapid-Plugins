package cz.vity.freerapid.plugins.services.happystreams;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class HappyStreamsServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getServiceTitle() {
        return "HappyStreams";
    }

    @Override
    public String getName() {
        return "happystreams.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new HappyStreamsFileRunner();
    }

}
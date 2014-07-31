package cz.vity.freerapid.plugins.services.beststreams;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class BestStreamsServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getServiceTitle() {
        return "BestStreams";
    }

    @Override
    public String getName() {
        return "beststreams.co";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new BestStreamsFileRunner();
    }

}
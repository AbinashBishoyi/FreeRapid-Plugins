package cz.vity.freerapid.plugins.services.bestreams;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class BestreamsServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getServiceTitle() {
        return "Bestreams";
    }

    @Override
    public String getName() {
        return "bestreams.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new BestreamsFileRunner();
    }

}
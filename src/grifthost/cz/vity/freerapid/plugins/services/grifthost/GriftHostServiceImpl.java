package cz.vity.freerapid.plugins.services.grifthost;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class GriftHostServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getServiceTitle() {
        return "GriftHost";
    }

    @Override
    public String getName() {
        return "grifthost.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new GriftHostFileRunner();
    }

}
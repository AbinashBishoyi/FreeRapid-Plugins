package cz.vity.freerapid.plugins.services.realvid;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class RealVidServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getServiceTitle() {
        return "RealVid";
    }

    @Override
    public String getName() {
        return "realvid.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new RealVidFileRunner();
    }

}
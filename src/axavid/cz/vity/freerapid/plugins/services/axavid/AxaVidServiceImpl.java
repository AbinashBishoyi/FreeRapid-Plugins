package cz.vity.freerapid.plugins.services.axavid;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class AxaVidServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getServiceTitle() {
        return "AxaVid";
    }

    @Override
    public String getName() {
        return "axavid.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new AxaVidFileRunner();
    }

}
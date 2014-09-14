package cz.vity.freerapid.plugins.services.skyvids;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class SkyVidsServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getServiceTitle() {
        return "SkyVids";
    }

    @Override
    public String getName() {
        return "skyvids.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SkyVidsFileRunner();
    }

}
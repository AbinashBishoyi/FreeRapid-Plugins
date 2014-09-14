package cz.vity.freerapid.plugins.services.fanso;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FansoServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getServiceTitle() {
        return "Fanso";
    }

    @Override
    public String getName() {
        return "fanso.tv";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FansoFileRunner();
    }

}
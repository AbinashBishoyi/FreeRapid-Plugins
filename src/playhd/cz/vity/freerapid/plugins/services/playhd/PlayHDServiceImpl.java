package cz.vity.freerapid.plugins.services.playhd;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class PlayHDServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getServiceTitle() {
        return "PlayHD";
    }

    @Override
    public String getName() {
        return "playhd.eu";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new PlayHDFileRunner();
    }

}
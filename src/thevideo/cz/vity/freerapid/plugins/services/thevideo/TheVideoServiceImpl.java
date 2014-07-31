package cz.vity.freerapid.plugins.services.thevideo;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class TheVideoServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getServiceTitle() {
        return "TheVideo";
    }

    @Override
    public String getName() {
        return "thevideo.me";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new TheVideoFileRunner();
    }

}
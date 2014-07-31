package cz.vity.freerapid.plugins.services.weshare;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class WeShareServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getServiceTitle() {
        return "WeShare";
    }

    @Override
    public String getName() {
        return "weshare.me";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new WeShareFileRunner();
    }

}
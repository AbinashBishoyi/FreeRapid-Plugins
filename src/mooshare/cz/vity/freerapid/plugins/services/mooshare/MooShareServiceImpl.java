package cz.vity.freerapid.plugins.services.mooshare;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class MooShareServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getServiceTitle() {
        return "MooShare";
    }

    @Override
    public String getName() {
        return "mooshare.biz";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MooShareFileRunner();
    }

}
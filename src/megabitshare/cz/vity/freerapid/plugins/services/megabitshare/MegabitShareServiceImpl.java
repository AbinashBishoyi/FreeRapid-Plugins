package cz.vity.freerapid.plugins.services.megabitshare;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class MegabitShareServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "MegabitShare";
    }

    @Override
    public String getName() {
        return "megabitshare.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MegabitShareFileRunner();
    }

}
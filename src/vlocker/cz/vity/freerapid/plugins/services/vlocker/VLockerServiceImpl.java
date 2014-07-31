package cz.vity.freerapid.plugins.services.vlocker;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class VLockerServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getServiceTitle() {
        return "VLocker";
    }

    @Override
    public String getName() {
        return "vlocker.org";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new VLockerFileRunner();
    }

}
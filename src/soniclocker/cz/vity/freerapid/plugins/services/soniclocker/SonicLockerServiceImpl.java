package cz.vity.freerapid.plugins.services.soniclocker;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class SonicLockerServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "SonicLocker";
    }

    @Override
    public String getName() {
        return "soniclocker.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SonicLockerFileRunner();
    }

}
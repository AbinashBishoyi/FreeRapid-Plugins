package cz.vity.freerapid.plugins.services.cyberlocker;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class CyberLockerServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "CyberLocker";
    }

    @Override
    public String getName() {
        return "cyberlocker.ch";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new CyberLockerFileRunner();
    }

}
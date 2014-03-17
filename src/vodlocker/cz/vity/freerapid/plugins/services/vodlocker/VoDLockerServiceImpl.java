package cz.vity.freerapid.plugins.services.vodlocker;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class VoDLockerServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "VoDLocker";
    }

    @Override
    public String getName() {
        return "vodlocker.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new VoDLockerFileRunner();
    }

}
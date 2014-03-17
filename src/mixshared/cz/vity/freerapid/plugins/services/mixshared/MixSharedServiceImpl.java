package cz.vity.freerapid.plugins.services.mixshared;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class MixSharedServiceImpl extends XFileSharingServiceImpl {
	
	@Override
    public String getServiceTitle() {
        return "MixShared";
    }
	
    @Override
    public String getName() {
        return "mixshared.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MixSharedFileRunner();
    }
}
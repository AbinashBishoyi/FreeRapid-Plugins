package cz.vity.freerapid.plugins.services.exclusivefaile;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class ExclusiveFaileServiceImpl extends XFileSharingServiceImpl {
	
	@Override
    public String getServiceTitle() {
        return "ExclusiveFaile";
    }
	
    @Override
    public String getName() {
        return "exclusivefaile.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ExclusiveFaileFileRunner();
    }
}
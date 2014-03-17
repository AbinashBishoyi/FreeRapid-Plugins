package cz.vity.freerapid.plugins.services.sharesix;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class ShareSixServiceImpl extends XFileSharingServiceImpl {
	
	@Override
    public String getServiceTitle() {
        return "ShareSix";
    }
	
    @Override
    public String getName() {
        return "sharesix.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ShareSixFileRunner();
    }
}
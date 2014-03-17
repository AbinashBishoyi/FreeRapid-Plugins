package cz.vity.freerapid.plugins.services.linkzhost;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class LinkzHostServiceImpl extends XFileSharingServiceImpl {
	
	@Override
    public String getServiceTitle() {
        return "LinkzHost";
    }
	
    @Override
    public String getName() {
        return "linkzhost.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new LinkzHostFileRunner();
    }
}
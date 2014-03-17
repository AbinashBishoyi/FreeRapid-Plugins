package cz.vity.freerapid.plugins.services.fourbytez;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class FourBytezServiceImpl extends XFileSharingServiceImpl {
	
	@Override
    public String getServiceTitle() {
        return "FourBytez";
    }
	
    @Override
    public String getName() {
        return "4bytez.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FourBytezFileRunner();
    }
}
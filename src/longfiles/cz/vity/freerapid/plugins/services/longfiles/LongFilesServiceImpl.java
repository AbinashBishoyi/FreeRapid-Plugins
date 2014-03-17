package cz.vity.freerapid.plugins.services.longfiles;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class LongFilesServiceImpl extends XFileSharingServiceImpl {
	
	@Override
    public String getServiceTitle() {
        return "LongFiles";
    }
	
    @Override
    public String getName() {
        return "longfiles.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new LongFilesFileRunner();
    }
}
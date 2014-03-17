package cz.vity.freerapid.plugins.services.filerose;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class FileRoseServiceImpl extends XFileSharingServiceImpl {
	
	@Override
    public String getServiceTitle() {
        return "FileRose";
    }
	
    @Override
    public String getName() {
        return "filerose.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileRoseFileRunner();
    }
}
package cz.vity.freerapid.plugins.services.uploadbeat;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class UploadBeatServiceImpl extends XFileSharingServiceImpl {
	
	@Override
    public String getServiceTitle() {
        return "UploadBeat";
    }
	
    @Override
    public String getName() {
        return "uploadbeat.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UploadBeatFileRunner();
    }
}
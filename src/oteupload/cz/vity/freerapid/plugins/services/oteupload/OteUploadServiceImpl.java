package cz.vity.freerapid.plugins.services.oteupload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class OteUploadServiceImpl extends XFileSharingServiceImpl {
	
	@Override
    public String getServiceTitle() {
        return "OteUpload";
    }
	
    @Override
    public String getName() {
        return "oteupload.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new OteUploadFileRunner();
    }
}
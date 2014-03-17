package cz.vity.freerapid.plugins.services.secureupload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class SecureUploadServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "SecureUpload";
    }

    @Override
    public String getName() {
        return "secureupload.eu";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SecureUploadFileRunner();
    }

}
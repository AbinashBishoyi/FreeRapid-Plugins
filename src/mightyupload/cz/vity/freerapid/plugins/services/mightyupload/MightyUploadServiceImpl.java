package cz.vity.freerapid.plugins.services.mightyupload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class MightyUploadServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "MightyUpload";
    }

    @Override
    public String getName() {
        return "mightyupload.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MightyUploadFileRunner();
    }

}
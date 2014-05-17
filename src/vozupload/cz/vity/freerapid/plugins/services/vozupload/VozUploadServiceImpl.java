package cz.vity.freerapid.plugins.services.vozupload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class VozUploadServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "VozUpload";
    }

    @Override
    public String getName() {
        return "vozupload.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new VozUploadFileRunner();
    }

}
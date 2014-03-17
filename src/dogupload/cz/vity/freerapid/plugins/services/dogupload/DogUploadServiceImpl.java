package cz.vity.freerapid.plugins.services.dogupload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class DogUploadServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "DogUpload";
    }

    @Override
    public String getName() {
        return "dogupload.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new DogUploadFileRunner();
    }

}
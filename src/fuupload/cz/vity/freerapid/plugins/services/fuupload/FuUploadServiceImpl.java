package cz.vity.freerapid.plugins.services.fuupload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FuUploadServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FuUpload";
    }

    @Override
    public String getName() {
        return "fuupload.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FuUploadFileRunner();
    }

}
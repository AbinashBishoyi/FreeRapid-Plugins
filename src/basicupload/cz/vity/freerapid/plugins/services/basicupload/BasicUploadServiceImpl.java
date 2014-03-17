package cz.vity.freerapid.plugins.services.basicupload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class BasicUploadServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "BasicUpload";
    }

    @Override
    public String getName() {
        return "basicupload.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new BasicUploadFileRunner();
    }

}
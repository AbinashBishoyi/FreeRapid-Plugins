package cz.vity.freerapid.plugins.services.hellupload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class HellUploadServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "HellUpload";
    }

    @Override
    public String getName() {
        return "hellupload.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new HellUploadFileRunner();
    }

}
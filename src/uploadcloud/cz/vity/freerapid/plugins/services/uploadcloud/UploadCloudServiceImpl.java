package cz.vity.freerapid.plugins.services.uploadcloud;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UploadCloudServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "UploadCloud";
    }

    @Override
    public String getName() {
        return "uploadcloud.pro";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UploadCloudFileRunner();
    }

}
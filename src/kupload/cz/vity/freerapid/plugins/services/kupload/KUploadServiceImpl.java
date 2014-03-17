package cz.vity.freerapid.plugins.services.kupload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class KUploadServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "KUpload";
    }

    @Override
    public String getName() {
        return "kupload.org";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new KUploadFileRunner();
    }
}
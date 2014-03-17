package cz.vity.freerapid.plugins.services.fiberupload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class FiberUploadServiceImpl extends XFileSharingServiceImpl {
    @Override
    public String getServiceTitle() {
        return "FiberUpload";
    }

    @Override
    public String getName() {
        return "fiberupload.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FiberUploadFileRunner();
    }
}
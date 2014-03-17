package cz.vity.freerapid.plugins.services.volupload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class VolUploadServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "VolUpload";
    }

    @Override
    public String getName() {
        return "volupload.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new VolUploadFileRunner();
    }

}
package cz.vity.freerapid.plugins.services.ok2upload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class OK2UploadServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "OK2Upload";
    }

    @Override
    public String getName() {
        return "ok2upload.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new OK2UploadFileRunner();
    }

}
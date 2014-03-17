package cz.vity.freerapid.plugins.services.files2upload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class Files2UploadServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "Files2Upload";
    }

    @Override
    public String getName() {
        return "files2upload.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new Files2UploadFileRunner();
    }

}
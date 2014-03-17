package cz.vity.freerapid.plugins.services.alfaupload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class AlfaUploadServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "AlfaUpload";
    }

    @Override
    public String getName() {
        return "alfaupload.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new AlfaUploadFileRunner();
    }

}
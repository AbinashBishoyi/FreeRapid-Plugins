package cz.vity.freerapid.plugins.services.icyupload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class IcyUploadServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "IcyUpload";
    }

    @Override
    public String getName() {
        return "icyupload.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new IcyUploadFileRunner();
    }

}
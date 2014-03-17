package cz.vity.freerapid.plugins.services.redeupload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class RedeUploadServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "RedeUpload";
    }

    @Override
    public String getName() {
        return "redeupload.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new RedeUploadFileRunner();
    }

}
package cz.vity.freerapid.plugins.services.uploadbaz;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UploadBazServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "UploadBaz";
    }

    @Override
    public String getName() {
        return "uploadbaz.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UploadBazFileRunner();
    }

}
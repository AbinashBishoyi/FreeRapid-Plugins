package cz.vity.freerapid.plugins.services.uploadcore;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UploadCoreServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "UploadCore";
    }

    @Override
    public String getName() {
        return "uploadcore.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UploadCoreFileRunner();
    }

}
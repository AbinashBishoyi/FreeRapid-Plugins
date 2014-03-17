package cz.vity.freerapid.plugins.services.uploadic;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UploadicServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "Uploadic";
    }

    @Override
    public String getName() {
        return "uploadic.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UploadicFileRunner();
    }

}
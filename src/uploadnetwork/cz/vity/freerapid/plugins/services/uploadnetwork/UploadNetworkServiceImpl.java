package cz.vity.freerapid.plugins.services.uploadnetwork;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UploadNetworkServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "UploadNetwork";
    }

    @Override
    public String getName() {
        return "uploadnetwork.eu";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UploadNetworkFileRunner();
    }

}
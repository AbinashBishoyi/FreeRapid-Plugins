package cz.vity.freerapid.plugins.services.uploadsat;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UploadSatServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "UploadSat";
    }

    @Override
    public String getName() {
        return "uploadsat.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UploadSatFileRunner();
    }

}
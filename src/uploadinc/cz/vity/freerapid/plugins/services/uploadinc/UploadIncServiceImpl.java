package cz.vity.freerapid.plugins.services.uploadinc;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UploadIncServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "UploadInc";
    }

    @Override
    public String getName() {
        return "uploadinc.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UploadIncFileRunner();
    }

}
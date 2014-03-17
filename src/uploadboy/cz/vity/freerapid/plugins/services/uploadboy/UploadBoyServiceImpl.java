package cz.vity.freerapid.plugins.services.uploadboy;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UploadBoyServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "UploadBoy";
    }

    @Override
    public String getName() {
        return "uploadboy.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UploadBoyFileRunner();
    }

}
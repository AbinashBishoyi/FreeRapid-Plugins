package cz.vity.freerapid.plugins.services.uploadrocket;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UploadRocketServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "UploadRocket";
    }

    @Override
    public String getName() {
        return "uploadrocket.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UploadRocketFileRunner();
    }

}
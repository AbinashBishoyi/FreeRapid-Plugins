package cz.vity.freerapid.plugins.services.uploadhunt;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UploadHuntServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "UploadHunt";
    }

    @Override
    public String getName() {
        return "uploadhunt.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UploadHuntFileRunner();
    }

}
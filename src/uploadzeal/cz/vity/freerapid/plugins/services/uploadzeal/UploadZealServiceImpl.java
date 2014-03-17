package cz.vity.freerapid.plugins.services.uploadzeal;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UploadZealServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "UploadZeal";
    }

    @Override
    public String getName() {
        return "uploadzeal.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UploadZealFileRunner();
    }

}
package cz.vity.freerapid.plugins.services.one80upload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class one80UploadServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "180Upload";
    }

    @Override
    public String getName() {
        return "180upload.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new one80UploadFileRunner();
    }

}
package cz.vity.freerapid.plugins.services.uploadscenter;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UploadsCenterServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "UploadsCenter";
    }

    @Override
    public String getName() {
        return "uploadscenter.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UploadsCenterFileRunner();
    }

}
package cz.vity.freerapid.plugins.services.uploadjet;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UploadJetServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "UploadJet";
    }

    @Override
    public String getName() {
        return "uploadjet.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UploadJetFileRunner();
    }

}
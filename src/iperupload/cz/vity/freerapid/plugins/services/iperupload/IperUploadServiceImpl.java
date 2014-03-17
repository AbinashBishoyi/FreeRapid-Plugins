package cz.vity.freerapid.plugins.services.iperupload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class IperUploadServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "IperUpload";
    }

    @Override
    public String getName() {
        return "iperupload.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new IperUploadFileRunner();
    }

}
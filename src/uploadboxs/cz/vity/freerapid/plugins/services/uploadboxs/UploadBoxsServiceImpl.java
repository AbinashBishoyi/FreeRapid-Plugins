package cz.vity.freerapid.plugins.services.uploadboxs;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UploadBoxsServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "UploadBoxs";
    }

    @Override
    public String getName() {
        return "uploadboxs.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UploadBoxsFileRunner();
    }

}
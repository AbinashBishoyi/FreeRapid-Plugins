package cz.vity.freerapid.plugins.services.dynaupload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class DynaUploadServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "DynaUpload";
    }

    @Override
    public String getName() {
        return "dynaupload.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new DynaUploadFileRunner();
    }

}
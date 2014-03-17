package cz.vity.freerapid.plugins.services.biggerupload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Vity
 */
public class BiggerUploadServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "BiggerUpload";
    }

    @Override
    public String getName() {
        return "biggerupload.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new BiggerUploadFileRunner();
    }

}

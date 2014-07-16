package cz.vity.freerapid.plugins.services.fastupload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FastUploadServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FastUpload";
    }

    @Override
    public String getName() {
        return "fastupload.org";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FastUploadFileRunner();
    }

}
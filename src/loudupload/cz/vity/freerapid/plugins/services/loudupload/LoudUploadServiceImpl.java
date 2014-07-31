package cz.vity.freerapid.plugins.services.loudupload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class LoudUploadServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "LoudUpload";
    }

    @Override
    public String getName() {
        return "loudupload.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new LoudUploadFileRunner();
    }
}
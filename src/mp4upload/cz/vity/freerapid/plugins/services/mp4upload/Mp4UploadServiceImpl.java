package cz.vity.freerapid.plugins.services.mp4upload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class Mp4UploadServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "Mp4Upload";
    }

    @Override
    public String getName() {
        return "mp4upload.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new Mp4UploadFileRunner();
    }

}
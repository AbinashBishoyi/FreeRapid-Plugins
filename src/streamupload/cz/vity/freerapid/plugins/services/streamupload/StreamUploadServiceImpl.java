package cz.vity.freerapid.plugins.services.streamupload;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class StreamUploadServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "streamupload.org";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new StreamUploadFileRunner();
    }

}
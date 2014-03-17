package cz.vity.freerapid.plugins.services.gotupload;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author Javi
 */
public class GotUploadServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "gotupload.com";
    }

    public int getMaxDownloadsFromOneIP() {
        return 2;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new GotUploadFileRunner();
    }

}
package cz.vity.freerapid.plugins.services.missupload;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class MissUploadServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "missupload.com";
    }

    public int getMaxDownloadsFromOneIP() {
        return 4;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MissUploadFileRunner();
    }

}
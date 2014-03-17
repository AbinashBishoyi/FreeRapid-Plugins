package cz.vity.freerapid.plugins.services.uploadspace;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author JPEXS
 */
public class UploadSpaceServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "uploadspace.eu";
    }

    public int getMaxDownloadsFromOneIP() {
        return 1;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UploadSpaceFileRunner();
    }

}

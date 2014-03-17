package cz.vity.freerapid.plugins.services.googlevideo;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author Vity
 */
public class GoogleVideoServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "googlevideo.com";
    }

    @Override
    public int getMaxDownloadsFromOneIP() {
        return 10;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new GoogleVideoFileRunner();
    }

}
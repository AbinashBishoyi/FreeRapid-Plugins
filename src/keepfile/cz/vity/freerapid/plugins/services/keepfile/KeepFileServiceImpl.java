package cz.vity.freerapid.plugins.services.keepfile;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author RickCL
 */
public class KeepFileServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "keepfile.com";
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
        return new KeepFileRunner();
    }

}
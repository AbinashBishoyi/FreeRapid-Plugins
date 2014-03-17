package cz.vity.freerapid.plugins.services.up_file;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author Thumb
 */
public class UpFileServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "up-file.com";
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
        return new UpFileFileRunner();
    }

}

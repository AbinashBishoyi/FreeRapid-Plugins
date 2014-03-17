package cz.vity.freerapid.plugins.services.file2box;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Vity
 */
public class File2BoxServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "file2box.com";
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
        return new File2BoxFileRunner();
    }

}

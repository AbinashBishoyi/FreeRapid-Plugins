package cz.vity.freerapid.plugins.services.filesonic;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author JPEXS
 */
public class FileSonicServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "filesonic.com";
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
        return new FileSonicFileRunner();
    }

}
package cz.vity.freerapid.plugins.services.divshare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Vity
 */
public class DivshareServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "divshare.com";
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
        return new DivshareFileRunner();
    }

}

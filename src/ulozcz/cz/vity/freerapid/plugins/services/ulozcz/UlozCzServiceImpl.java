package cz.vity.freerapid.plugins.services.ulozcz;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Vity
 *         Class that provides basic info about plugin
 */
public class UlozCzServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "uloz.cz";

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 9;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UlozCzFileRunner();
    }

}

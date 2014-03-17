package cz.vity.freerapid.plugins.services.forusto;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author Javi
 */
public class ForUsToServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "4us.to";
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
        return new ForUsToFileRunner();
    }

}
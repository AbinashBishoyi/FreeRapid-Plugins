package cz.vity.freerapid.plugins.services.kewlshare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author Arthur Gunawan
 */
public class KewlshareServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "kewlshare.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new KewlshareFileRunner();
    }

}

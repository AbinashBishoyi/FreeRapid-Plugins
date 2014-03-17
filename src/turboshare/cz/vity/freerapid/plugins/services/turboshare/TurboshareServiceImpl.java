package cz.vity.freerapid.plugins.services.turboshare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author Arthur Gunawan
 */
public class TurboshareServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "turboshare.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new TurboshareFileRunner();
    }

}

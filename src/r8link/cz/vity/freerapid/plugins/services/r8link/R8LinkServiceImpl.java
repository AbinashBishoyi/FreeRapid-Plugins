package cz.vity.freerapid.plugins.services.r8link;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class R8LinkServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "r8link.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new R8LinkFileRunner();
    }

}
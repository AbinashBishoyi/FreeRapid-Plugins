package cz.vity.freerapid.plugins.services.f2h_co_il;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class F2h_co_ilServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "f2h.co.il";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new F2h_co_ilFileRunner();
    }

}
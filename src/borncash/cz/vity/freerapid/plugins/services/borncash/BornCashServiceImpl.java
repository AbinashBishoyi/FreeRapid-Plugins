package cz.vity.freerapid.plugins.services.borncash;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class BornCashServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "borncash.org";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new BornCashFileRunner();
    }

}
package cz.vity.freerapid.plugins.services.coinurl;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class CoinUrlServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "coinurl.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new CoinUrlFileRunner();
    }

}
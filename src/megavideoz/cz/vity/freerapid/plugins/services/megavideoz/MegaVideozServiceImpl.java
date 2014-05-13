package cz.vity.freerapid.plugins.services.megavideoz;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class MegaVideozServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "megavideoz.eu";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MegaVideozFileRunner();
    }

}
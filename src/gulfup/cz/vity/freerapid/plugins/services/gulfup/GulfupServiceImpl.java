package cz.vity.freerapid.plugins.services.gulfup;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class GulfupServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "gulfup.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new GulfupFileRunner();
    }

}
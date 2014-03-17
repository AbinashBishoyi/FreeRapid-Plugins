package cz.vity.freerapid.plugins.services.lnx_lu;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class Lnx_LuServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "lnx.lu";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new Lnx_LuFileRunner();
    }

}
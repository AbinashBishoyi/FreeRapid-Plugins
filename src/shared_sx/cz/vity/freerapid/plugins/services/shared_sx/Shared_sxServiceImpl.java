package cz.vity.freerapid.plugins.services.shared_sx;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class Shared_sxServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "shared.sx";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new Shared_sxFileRunner();
    }

}
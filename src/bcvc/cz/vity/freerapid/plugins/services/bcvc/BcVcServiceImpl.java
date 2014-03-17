package cz.vity.freerapid.plugins.services.bcvc;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class BcVcServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "bc.vc";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new BcVcFileRunner();
    }

}
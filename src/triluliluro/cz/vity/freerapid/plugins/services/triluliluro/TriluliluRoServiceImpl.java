package cz.vity.freerapid.plugins.services.triluliluro;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class TriluliluRoServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "trilulilu.ro";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new TriluliluRoFileRunner();
    }

}
package cz.vity.freerapid.plugins.services.dramafever;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class DramaFeverServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "dramafever.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new DramaFeverFileRunner();
    }

}
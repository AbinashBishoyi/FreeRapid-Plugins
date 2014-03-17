package cz.vity.freerapid.plugins.services.u115;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Meow
 */
public class u115ServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "u.115.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new u115FileRunner();
    }

}
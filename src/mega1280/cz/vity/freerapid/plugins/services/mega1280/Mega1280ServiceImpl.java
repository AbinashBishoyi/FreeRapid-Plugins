package cz.vity.freerapid.plugins.services.mega1280;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Vity
 */
public class Mega1280ServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "mega1280.com";
    }


    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new Mega1280FileRunner();
    }

}
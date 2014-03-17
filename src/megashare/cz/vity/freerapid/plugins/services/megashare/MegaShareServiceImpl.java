package cz.vity.freerapid.plugins.services.megashare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class MegaShareServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "megashare.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MegaShareFileRunner();
    }

}
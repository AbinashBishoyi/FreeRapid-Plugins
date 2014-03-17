package cz.vity.freerapid.plugins.services.flashx;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class FlashXServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "flashx.tv";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FlashXFileRunner();
    }

}
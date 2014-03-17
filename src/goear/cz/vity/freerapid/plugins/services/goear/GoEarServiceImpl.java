package cz.vity.freerapid.plugins.services.goear;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class GoEarServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "goear.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new GoEarFileRunner();
    }

}
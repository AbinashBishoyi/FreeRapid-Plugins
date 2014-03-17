package cz.vity.freerapid.plugins.services.thewb;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class TheWbServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "thewb.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new TheWbFileRunner();
    }

}
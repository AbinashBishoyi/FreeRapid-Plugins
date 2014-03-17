package cz.vity.freerapid.plugins.services.nbc;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class NbcServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "nbc.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new NbcFileRunner();
    }

}
package cz.vity.freerapid.plugins.services.crackle;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class CrackleServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "crackle.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new CrackleFileRunner();
    }

}
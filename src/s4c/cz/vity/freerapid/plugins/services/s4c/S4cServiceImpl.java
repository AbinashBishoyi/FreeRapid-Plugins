package cz.vity.freerapid.plugins.services.s4c;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class S4cServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "s4c.co.uk";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new S4cFileRunner();
    }

}
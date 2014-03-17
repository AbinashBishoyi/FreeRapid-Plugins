package cz.vity.freerapid.plugins.services.gett;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class GettServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "ge.tt";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new GettFileRunner();
    }

}
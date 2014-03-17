package cz.vity.freerapid.plugins.services.hollywoodtuna;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Alex
 */
public class HollywoodtunaServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "hollywoodtuna.com";

    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new HollywoodtunaRunner();
    }

}

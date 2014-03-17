package cz.vity.freerapid.plugins.services.yourfilehost;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Alex
 */
public class YourFileHostServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "yourfilehost.com";

    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new YourFileHostRunner();
    }

}

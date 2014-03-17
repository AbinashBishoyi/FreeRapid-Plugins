package cz.vity.freerapid.plugins.services.jandown;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Alex
 */
public class JandownServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "jandown.com";

    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new JandownRunner();
    }

}

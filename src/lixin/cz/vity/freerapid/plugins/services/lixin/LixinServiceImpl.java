package cz.vity.freerapid.plugins.services.lixin;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Alex
 */
public class LixinServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "lix.in";

    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new LixinRunner();
    }

}

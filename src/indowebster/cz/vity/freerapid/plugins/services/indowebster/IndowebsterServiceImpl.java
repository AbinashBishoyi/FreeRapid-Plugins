package cz.vity.freerapid.plugins.services.indowebster;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Alex
 */
public class IndowebsterServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "indowebster.com";

    @Override
    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new IndowebsterRunner();
    }

}

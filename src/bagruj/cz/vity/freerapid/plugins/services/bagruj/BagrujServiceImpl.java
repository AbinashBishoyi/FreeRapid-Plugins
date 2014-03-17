package cz.vity.freerapid.plugins.services.bagruj;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
public class BagrujServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "bagruj.cz";

    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new BagrujRunner();
    }
}
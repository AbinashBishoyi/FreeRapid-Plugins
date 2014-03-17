package cz.vity.freerapid.plugins.services.usercash;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Ladislav Vitasek
 */
public class UserCashServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "usercash.com";

    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UserCashRunner();
    }

}

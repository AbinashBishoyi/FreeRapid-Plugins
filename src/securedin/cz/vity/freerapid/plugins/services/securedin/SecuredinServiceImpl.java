package cz.vity.freerapid.plugins.services.securedin;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author RickCL
 */
public class SecuredinServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "secured.in";

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SecuredinRunner();
    }

    @Override
    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

}

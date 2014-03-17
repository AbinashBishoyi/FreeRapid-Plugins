package cz.vity.freerapid.plugins.services.cryptit;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author RickCL
 */
public class CryptItServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "cryptit.com";

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new CryptItRunner();
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

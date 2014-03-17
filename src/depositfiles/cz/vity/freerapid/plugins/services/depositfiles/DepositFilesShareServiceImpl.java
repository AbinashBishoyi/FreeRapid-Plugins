package cz.vity.freerapid.plugins.services.depositfiles;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Ladislav Vitasek
 */
public class DepositFilesShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "depositfiles.com";

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
        return new DepositFilesRunner();
    }


}
package cz.vity.freerapid.plugins.services.fileflyer;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Kajda
 */
public class FileFlyerServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "fileflyer.com";

    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileFlyerFileRunner();
    }
}
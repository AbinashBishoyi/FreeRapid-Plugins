package cz.vity.freerapid.plugins.services.storeplace;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author RickCL
 */
public class StoreplaceFilesShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "storeplace.org";

    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new StoreplaceFilesRunner();
    }


}
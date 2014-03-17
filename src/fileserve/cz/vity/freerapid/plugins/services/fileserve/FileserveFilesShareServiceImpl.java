package cz.vity.freerapid.plugins.services.fileserve;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author RickCL
 */
public class FileserveFilesShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "fileserve.com";

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
        return new FileserveFilesRunner();
    }


}
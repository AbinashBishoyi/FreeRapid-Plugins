package cz.vity.freerapid.plugins.services.filestore;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author RickCL
 */
public class FilestoreFilesShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "filestore.to";

    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FilestoreFilesRunner();
    }


}
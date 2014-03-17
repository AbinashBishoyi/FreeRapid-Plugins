package cz.vity.freerapid.plugins.services.uploadhere;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Heend
 *         Class that provides basic info about plugin
 */
public class UploadhereServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "uploadhere.com";

    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UploadhereRunner();
    }

}

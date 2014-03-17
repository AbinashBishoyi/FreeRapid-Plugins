package cz.vity.freerapid.plugins.services.uploadking;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Heend
 *         Class that provides basic info about plugin
 */
public class UploadkingServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "uploadking.com";

    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UploadkingRunner();
    }

}

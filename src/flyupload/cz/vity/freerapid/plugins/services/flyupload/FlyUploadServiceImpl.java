package cz.vity.freerapid.plugins.services.flyupload;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Vity
 *         Class that provides basic info about plugin
 */
public class FlyUploadServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "flyupload";

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 8;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FlyUploadRunner();
    }

}

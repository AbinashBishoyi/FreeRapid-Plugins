package cz.vity.freerapid.plugins.services.uploadedto;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Ladislav Vitasek
 */
public class UploadedToShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "uploaded.to";

    @Override
    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public int getMaxDownloadsFromOneIP() {
        return 1;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UploadedToRunner();
    }

}

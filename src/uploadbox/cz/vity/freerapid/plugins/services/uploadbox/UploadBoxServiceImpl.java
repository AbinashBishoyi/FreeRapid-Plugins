package cz.vity.freerapid.plugins.services.uploadbox;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Kajda
 */
public class UploadBoxServiceImpl extends AbstractFileShareService {
    private final static String SERVICE_NAME = "uploadbox.com";

    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UploadBoxFileRunner();
    }
}

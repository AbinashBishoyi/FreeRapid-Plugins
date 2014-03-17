package cz.vity.freerapid.plugins.services.upload;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Saikek
 */
public class UploadShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "upload.com.ua";

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 5; //? i don't know yet
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UploadRunner();
    }


}
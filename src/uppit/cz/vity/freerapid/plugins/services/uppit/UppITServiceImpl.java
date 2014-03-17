package cz.vity.freerapid.plugins.services.uppit;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Vity
 */
public class UppITServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "wiiupload.net";

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 9;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UppITFileRunner();
    }

}

package cz.vity.freerapid.plugins.services.filesend;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Ladislav Vitasek
 */
public class FileSendServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "filesend.net";

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 8; //I checked that before
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileSendFileRunner();
    }

}

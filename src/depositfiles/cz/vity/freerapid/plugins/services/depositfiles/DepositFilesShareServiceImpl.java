package cz.vity.freerapid.plugins.services.depositfiles;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.HttpFileDownloader;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Ladislav Vitasek
 */
public class DepositFilesShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "depositfiles.com";

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 1;
    }

        @Override
    public boolean supportsRunCheck() {
        return true;
    }

        @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new DepositFilesRunner();
    }
  

}
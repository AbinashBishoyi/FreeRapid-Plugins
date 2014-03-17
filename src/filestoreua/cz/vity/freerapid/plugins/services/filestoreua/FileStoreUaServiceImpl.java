package cz.vity.freerapid.plugins.services.filestoreua;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Saikek
 *         Class that provides basic info about plugin
 */
 public class FileStoreUaServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "filestore.com.ua";

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 8;//TODO - check!!!
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//TODO - check!!!
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FileStoreUaFileRunner();
    }

}

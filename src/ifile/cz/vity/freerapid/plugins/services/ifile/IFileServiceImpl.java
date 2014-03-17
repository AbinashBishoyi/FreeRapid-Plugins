package cz.vity.freerapid.plugins.services.ifile;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Kajda
 */
public class IFileServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "ifile.it";

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 3;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new IFileFileRunner();
    }
}
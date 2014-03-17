package cz.vity.freerapid.plugins.services.ifile;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author JPEXS
 */
public class IFileServiceImpl extends AbstractFileShareService {
    private final static String SERVICE_NAME = "ifile.it";

    @Override
    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public int getMaxDownloadsFromOneIP() {
        return 2;
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
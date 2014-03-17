package cz.vity.freerapid.plugins.services.filesdump;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Vity
 *         Class that provides basic info about plugin
 */
public class FilesDumpServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "filesdump.com";

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 1;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FilesDumpRunner();
    }

}

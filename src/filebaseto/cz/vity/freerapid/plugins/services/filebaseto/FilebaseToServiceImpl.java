package cz.vity.freerapid.plugins.services.filebaseto;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Ladislav Vitasek
 */
public class FilebaseToServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "filebase.to";


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
        return new FilebaseToRunner();
    }

}
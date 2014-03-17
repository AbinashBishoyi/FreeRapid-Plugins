package cz.vity.freerapid.plugins.services.filebitnet;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author RickCL
 */
public class FilebitNetFilesShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "file-bit.net";

    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FilebitNetFilesRunner();
    }


}
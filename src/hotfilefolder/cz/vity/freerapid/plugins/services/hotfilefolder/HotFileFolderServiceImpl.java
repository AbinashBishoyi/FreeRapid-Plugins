package cz.vity.freerapid.plugins.services.hotfilefolder;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author RickCL
 */
public class HotFileFolderServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "hotfilefolder";

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new HotFileFolderRunner();
    }

    @Override
    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

}

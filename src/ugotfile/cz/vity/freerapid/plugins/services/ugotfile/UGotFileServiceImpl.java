package cz.vity.freerapid.plugins.services.ugotfile;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Kajda
 */
public class UGotFileServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "ugotfile.com";

    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UGotFileFileRunner();
    }
}

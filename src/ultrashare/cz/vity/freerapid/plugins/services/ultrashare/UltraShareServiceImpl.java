package cz.vity.freerapid.plugins.services.ultrashare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Kajda
 */
public class UltraShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "ultrashare.net";

    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UltraShareFileRunner();
    }
}
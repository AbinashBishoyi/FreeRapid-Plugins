package cz.vity.freerapid.plugins.services.zippyshare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Kajda
 */
public class ZippyShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "zippyshare.com";

    @Override
    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ZippyShareFileRunner();
    }
}
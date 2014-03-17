package cz.vity.freerapid.plugins.services.gigasize;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Kajda
 */
public class GigaSizeServiceImpl extends AbstractFileShareService {
    private final static String SERVICE_NAME = "gigasize.com";

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
        return new GigaSizeFileRunner();
    }
}

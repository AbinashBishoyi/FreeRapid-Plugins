package cz.vity.freerapid.plugins.services.sdilej;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Kajda
 */
public class SdilejServiceImpl extends AbstractFileShareService {
    private final static String SERVICE_NAME = "sdilej.to";

    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SdilejFileRunner();
    }
}

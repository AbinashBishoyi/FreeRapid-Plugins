package cz.vity.freerapid.plugins.services.nahrajcz;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Vity
 *         Class that provides basic info about plugin
 */
public class NahrajCzServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "nahraj.cz";

    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new NahrajCzFileRunner();
    }

}

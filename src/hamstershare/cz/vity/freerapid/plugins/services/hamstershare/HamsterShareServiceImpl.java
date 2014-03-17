package cz.vity.freerapid.plugins.services.hamstershare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Vookimedlo
 */
public class HamsterShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "hamstershare.com";

    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new HamsterShareFileRunner();
    }

}

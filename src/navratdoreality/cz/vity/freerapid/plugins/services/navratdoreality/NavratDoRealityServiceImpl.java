package cz.vity.freerapid.plugins.services.navratdoreality;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author iki
 */
public class NavratDoRealityServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "navratdoreality.cz";

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
        return new NavratDoRealityFileRunner();
    }

}
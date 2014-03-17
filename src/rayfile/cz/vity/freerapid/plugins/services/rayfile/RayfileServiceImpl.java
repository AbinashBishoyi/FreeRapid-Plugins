package cz.vity.freerapid.plugins.services.rayfile;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Tommy Yang
 */
public class RayfileServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "ctdisk.com";

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
        return new RayfileRunner();
    }

}

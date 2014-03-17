package cz.vity.freerapid.plugins.services.servupcoil;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Frishrash
 */
public class ServUpCoIlServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "servup.co.il";

    @Override
    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ServUpCoIlFileRunner();
    }

}
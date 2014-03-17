package cz.vity.freerapid.plugins.services.zshare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;


/**
 * @author Prathap Reddy
 */
public class ZShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "zshare.net";

    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ZShareRunner();
    }
}
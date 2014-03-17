package cz.vity.freerapid.plugins.services.qjwm;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Tommy Yang
 */
public class QjwmServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "qjwm.com";

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
        return new QjwmRunner();
    }

}

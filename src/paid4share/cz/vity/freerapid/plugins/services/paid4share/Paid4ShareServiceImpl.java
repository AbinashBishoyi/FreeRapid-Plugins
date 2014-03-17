package cz.vity.freerapid.plugins.services.paid4share;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Vity
 */
public class Paid4ShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "paid4share.com";

    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new Paid4ShareRunner();
    }

}

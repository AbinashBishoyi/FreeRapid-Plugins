package cz.vity.freerapid.plugins.services.toshared;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Tiago Hillebrandt <tiagohillebrandt@gmail.com>
 */
public class ToSharedServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "2shared.com";

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
        return new ToSharedRunner();
    }

}

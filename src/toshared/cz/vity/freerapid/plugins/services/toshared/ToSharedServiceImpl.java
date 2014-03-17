package cz.vity.freerapid.plugins.services.toshared;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Tiago Hillebrandt <tiagohillebrandt@gmail.com>
 */
public class ToSharedServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "2shared.com";


    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 1;
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

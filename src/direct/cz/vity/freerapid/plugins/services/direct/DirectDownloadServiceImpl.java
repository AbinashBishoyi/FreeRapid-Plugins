package cz.vity.freerapid.plugins.services.direct;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author ntoskrnl
 */
public class DirectDownloadServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "direct";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new DirectDownloadRunner();
    }

}
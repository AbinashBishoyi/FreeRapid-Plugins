package cz.vity.freerapid.plugins.services.wupload;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Coloss
 */
public class WuploadShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "wupload.com";

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
        return new WuploadRunner();
    }


}
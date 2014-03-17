package cz.vity.freerapid.plugins.services.megaupload;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Ladislav Vitasek
 */
public class MegauploadShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "megaupload.com";

    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MegauploadRunner();
    }

}
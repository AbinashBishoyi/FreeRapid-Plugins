package cz.vity.freerapid.plugins.services.enterupload;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;


/**
 * @author Ladislav Vitasek
 */
public class EnteruploadServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "enterupload.com";

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
        return new EnteruploadRunner();
    }
}
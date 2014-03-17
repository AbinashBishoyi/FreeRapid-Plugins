package cz.vity.freerapid.plugins.services.mediafire;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
public class MediafireServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "mediafire.com";

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
        return new MediafireRunner();
    }


}
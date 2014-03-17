package cz.vity.freerapid.plugins.services.linkbucks;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Alex
 */
public class LinkBucksServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "linkbucks.com";

    @Override
    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new LinkBucksRunner();
    }

}

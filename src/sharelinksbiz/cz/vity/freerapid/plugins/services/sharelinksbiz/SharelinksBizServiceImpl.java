package cz.vity.freerapid.plugins.services.sharelinksbiz;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author RickCL
 */
public class SharelinksBizServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "share-links.biz";

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SharelinksBizRunner();
    }

    @Override
    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

}

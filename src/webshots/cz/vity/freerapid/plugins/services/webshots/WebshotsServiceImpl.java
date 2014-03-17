package cz.vity.freerapid.plugins.services.webshots;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class WebshotsServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "webshots.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new WebshotsFileRunner();
    }

}
package cz.vity.freerapid.plugins.services.twogbhosting;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class TwoGbHostingServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "2gb-hosting.com ";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new TwoGbHostingFileRunner();
    }

}
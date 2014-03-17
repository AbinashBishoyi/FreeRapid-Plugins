package cz.vity.freerapid.plugins.services.safeurl;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class SafeUrlServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "safeurl.me";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SafeUrlFileRunner();
    }

}
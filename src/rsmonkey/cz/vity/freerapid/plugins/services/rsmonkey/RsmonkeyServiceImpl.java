package cz.vity.freerapid.plugins.services.rsmonkey;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author Arthur Gunawan
 */
public class RsmonkeyServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "rsmonkey.com";
    }

    public int getMaxDownloadsFromOneIP() {
        return 10;
    }

    @Override
    public boolean supportsRunCheck() {
        return false;//Check not supported
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new RsmonkeyFileRunner();
    }

}

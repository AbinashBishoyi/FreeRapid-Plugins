package cz.vity.freerapid.plugins.services.usershare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Vity
 */
public class UserShareServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "usershare.net";
    }

    public int getMaxDownloadsFromOneIP() {
        return 3;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UserShareFileRunner();
    }

}

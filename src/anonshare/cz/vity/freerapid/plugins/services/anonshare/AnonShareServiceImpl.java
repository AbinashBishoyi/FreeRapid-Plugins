package cz.vity.freerapid.plugins.services.anonshare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class AnonShareServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "anonshare.org";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new AnonShareFileRunner();
    }

}
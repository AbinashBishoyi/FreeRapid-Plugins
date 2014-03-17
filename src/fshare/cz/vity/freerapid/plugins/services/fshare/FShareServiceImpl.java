package cz.vity.freerapid.plugins.services.fshare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FShareServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "fshare.vn";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FShareFileRunner();
    }

}
package cz.vity.freerapid.plugins.services.fodashare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FodaShareServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "fodashare.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FodaShareFileRunner();
    }

}
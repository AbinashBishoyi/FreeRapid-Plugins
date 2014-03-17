package cz.vity.freerapid.plugins.services.tv4play;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class Tv4PlayServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "tv4play.se";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new Tv4PlayFileRunner();
    }

}
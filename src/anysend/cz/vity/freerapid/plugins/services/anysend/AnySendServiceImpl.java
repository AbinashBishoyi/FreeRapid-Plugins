package cz.vity.freerapid.plugins.services.anysend;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class AnySendServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "anysend.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new AnySendFileRunner();
    }

}
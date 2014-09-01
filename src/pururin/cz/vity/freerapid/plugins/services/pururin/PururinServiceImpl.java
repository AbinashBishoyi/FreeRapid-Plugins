package cz.vity.freerapid.plugins.services.pururin;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class PururinServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "pururin.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new PururinFileRunner();
    }

}
package cz.vity.freerapid.plugins.services.cloudzilla;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class CloudZillaServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "cloudzilla.to";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new CloudZillaFileRunner();
    }

}
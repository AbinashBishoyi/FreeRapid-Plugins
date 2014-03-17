package cz.vity.freerapid.plugins.services.fsakura;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FsakuraServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "Fsakura";
    }

    @Override
    public String getName() {
        return "fsakura.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FsakuraFileRunner();
    }

}
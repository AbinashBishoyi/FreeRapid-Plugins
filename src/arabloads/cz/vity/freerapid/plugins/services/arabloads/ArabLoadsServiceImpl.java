package cz.vity.freerapid.plugins.services.arabloads;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class ArabLoadsServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "ArabLoads";
    }

    @Override
    public String getName() {
        return "arabloads.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ArabLoadsFileRunner();
    }

}
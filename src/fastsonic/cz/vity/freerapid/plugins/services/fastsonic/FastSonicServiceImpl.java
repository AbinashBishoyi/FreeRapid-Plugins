package cz.vity.freerapid.plugins.services.fastsonic;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FastSonicServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FastSonic";
    }

    @Override
    public String getName() {
        return "fastsonic.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FastSonicFileRunner();
    }

}
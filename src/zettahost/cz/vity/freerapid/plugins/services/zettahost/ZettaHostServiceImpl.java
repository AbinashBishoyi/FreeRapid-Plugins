package cz.vity.freerapid.plugins.services.zettahost;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class ZettaHostServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "ZettaHost";
    }

    @Override
    public String getName() {
        return "zettahost.tv";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ZettaHostFileRunner();
    }

}
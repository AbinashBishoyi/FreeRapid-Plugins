package cz.vity.freerapid.plugins.services.redbunker;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class RedBunkerServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "RedBunker";
    }

    @Override
    public String getName() {
        return "redbunker.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new RedBunkerFileRunner();
    }

}
package cz.vity.freerapid.plugins.services.youwatch;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class YouWatchServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "YouWatch";
    }

    @Override
    public String getName() {
        return "youwatch.org";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new YouWatchFileRunner();
    }

}
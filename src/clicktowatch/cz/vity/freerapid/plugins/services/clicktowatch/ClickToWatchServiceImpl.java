package cz.vity.freerapid.plugins.services.clicktowatch;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class ClickToWatchServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "ClickToWatch";
    }

    @Override
    public String getName() {
        return "clicktowatch.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ClickToWatchFileRunner();
    }

}
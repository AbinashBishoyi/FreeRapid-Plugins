package cz.vity.freerapid.plugins.services.sharexvid;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class ShareXvidServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "ShareXvid";
    }

    @Override
    public String getName() {
        return "sharexvid.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ShareXvidFileRunner();
    }

}
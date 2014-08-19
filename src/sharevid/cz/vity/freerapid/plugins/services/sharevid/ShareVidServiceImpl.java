package cz.vity.freerapid.plugins.services.sharevid;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class ShareVidServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "ShareVid";
    }

    @Override
    public String getName() {
        return "sharevid.org";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ShareVidFileRunner();
    }

}
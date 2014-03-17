package cz.vity.freerapid.plugins.services.shareblue;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class ShareBlueServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "ShareBlue";
    }

    @Override
    public String getName() {
        return "shareblue.eu";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ShareBlueFileRunner();
    }

}
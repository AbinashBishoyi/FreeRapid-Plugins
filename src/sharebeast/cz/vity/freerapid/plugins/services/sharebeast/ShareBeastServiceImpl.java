package cz.vity.freerapid.plugins.services.sharebeast;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class ShareBeastServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "ShareBeast";
    }

    @Override
    public String getName() {
        return "sharebeast.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ShareBeastFileRunner();
    }

}
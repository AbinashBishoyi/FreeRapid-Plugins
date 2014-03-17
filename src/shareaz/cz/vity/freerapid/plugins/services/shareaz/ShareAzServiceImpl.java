package cz.vity.freerapid.plugins.services.shareaz;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class ShareAzServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "ShareAz";
    }

    @Override
    public String getName() {
        return "share.az";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ShareAzFileRunner();
    }

}
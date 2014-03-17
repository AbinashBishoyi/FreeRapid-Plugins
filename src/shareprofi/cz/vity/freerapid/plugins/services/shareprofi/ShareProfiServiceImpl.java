package cz.vity.freerapid.plugins.services.shareprofi;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class ShareProfiServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "ShareProfi";
    }

    @Override
    public String getName() {
        return "shareprofi.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ShareProfiFileRunner();
    }

}
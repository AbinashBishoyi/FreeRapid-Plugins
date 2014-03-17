package cz.vity.freerapid.plugins.services.sharefiles;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class ShareFilesServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "ShareFiles";
    }

    @Override
    public String getName() {
        return "sharefiles.co";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ShareFilesFileRunner();
    }

}
package cz.vity.freerapid.plugins.services.sharerepo;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class ShareRepoServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "ShareRepo";
    }

    @Override
    public String getName() {
        return "sharerepo.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ShareRepoFileRunner();
    }

}
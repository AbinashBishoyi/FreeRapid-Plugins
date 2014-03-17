package cz.vity.freerapid.plugins.services.bzlink;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class BZlinkServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "BZlink";
    }

    @Override
    public String getName() {
        return "bzlink.us";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new BZlinkFileRunner();
    }

}
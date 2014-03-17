package cz.vity.freerapid.plugins.services.putshare;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class putShareServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "putShare";
    }

    @Override
    public String getName() {
        return "putshare.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new putShareFileRunner();
    }

}
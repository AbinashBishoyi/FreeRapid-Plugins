package cz.vity.freerapid.plugins.services.crisshare;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class CrisShareServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "CrisShare";
    }

    @Override
    public String getName() {
        return "crisshare.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new CrisShareFileRunner();
    }

}
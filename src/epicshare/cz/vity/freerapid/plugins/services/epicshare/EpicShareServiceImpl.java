package cz.vity.freerapid.plugins.services.epicshare;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class EpicShareServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "EpicShare";
    }

    @Override
    public String getName() {
        return "epicshare.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new EpicShareFileRunner();
    }

}
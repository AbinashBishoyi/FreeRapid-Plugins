package cz.vity.freerapid.plugins.services.muchshare;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class MuchShareServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "MuchShare";
    }

    @Override
    public String getName() {
        return "muchshare.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MuchShareFileRunner();
    }
}
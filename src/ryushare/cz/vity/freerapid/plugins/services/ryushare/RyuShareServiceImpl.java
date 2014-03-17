package cz.vity.freerapid.plugins.services.ryushare;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class RyuShareServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "RyuShare";
    }

    @Override
    public String getName() {
        return "ryushare.com";
    }

    @Override
    public PluginRunner getPluginRunnerInstance() {
        return new RyuShareFileRunner();
    }

}
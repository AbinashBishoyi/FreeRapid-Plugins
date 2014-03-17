package cz.vity.freerapid.plugins.services.movzap;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class MovZapServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "MovZap";
    }

    @Override
    public String getName() {
        return "movzap.com";
    }

    @Override
    public PluginRunner getPluginRunnerInstance() {
        return new MovZapFileRunner();
    }

}
package cz.vity.freerapid.plugins.services.speedshare;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class SpeedShareServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "SpeedShare";
    }

    @Override
    public String getName() {
        return "speedshare.eu";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SpeedShareFileRunner();
    }

}
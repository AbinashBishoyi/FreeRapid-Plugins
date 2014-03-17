package cz.vity.freerapid.plugins.services.movreel;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class MovreelServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "MovReel.com";
    }

    @Override
    public String getName() {
        return "movreel.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MovreelFileRunner();
    }

}
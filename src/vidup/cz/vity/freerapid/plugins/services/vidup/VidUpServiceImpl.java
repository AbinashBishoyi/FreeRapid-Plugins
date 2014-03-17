package cz.vity.freerapid.plugins.services.vidup;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class VidUpServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "VidUp";
    }

    @Override
    public String getName() {
        return "vidup.me";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new VidUpFileRunner();
    }

}
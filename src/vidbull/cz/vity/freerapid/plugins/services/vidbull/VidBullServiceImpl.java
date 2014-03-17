package cz.vity.freerapid.plugins.services.vidbull;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class VidBullServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "VidBull";
    }

    @Override
    public String getName() {
        return "vidbull.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new VidBullFileRunner();
    }

}
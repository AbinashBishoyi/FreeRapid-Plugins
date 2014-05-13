package cz.vity.freerapid.plugins.services.vidplay;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class VidPlayServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "VidPlay";
    }

    @Override
    public String getName() {
        return "vidplay.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new VidPlayFileRunner();
    }

}
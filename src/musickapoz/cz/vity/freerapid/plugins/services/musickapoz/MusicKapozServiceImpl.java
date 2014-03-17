package cz.vity.freerapid.plugins.services.musickapoz;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class MusicKapozServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "MusicKapoz";
    }

    @Override
    public String getName() {
        return "musickapoz.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MusicKapozFileRunner();
    }

}
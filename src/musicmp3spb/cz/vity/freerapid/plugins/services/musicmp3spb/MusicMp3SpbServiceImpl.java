package cz.vity.freerapid.plugins.services.musicmp3spb;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class MusicMp3SpbServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "musicmp3spb.org";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MusicMp3SpbFileRunner();
    }

}
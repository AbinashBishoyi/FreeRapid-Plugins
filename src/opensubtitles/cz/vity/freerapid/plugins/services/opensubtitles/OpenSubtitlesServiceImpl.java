package cz.vity.freerapid.plugins.services.opensubtitles;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class OpenSubtitlesServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "opensubtitles.org";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new OpenSubtitlesFileRunner();
    }

}
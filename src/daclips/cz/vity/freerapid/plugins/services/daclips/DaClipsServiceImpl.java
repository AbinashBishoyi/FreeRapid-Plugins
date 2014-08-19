package cz.vity.freerapid.plugins.services.daclips;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class DaClipsServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getServiceTitle() {
        return "DaClips";
    }

    @Override
    public String getName() {
        return "daclips.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new DaClipsFileRunner();
    }

}
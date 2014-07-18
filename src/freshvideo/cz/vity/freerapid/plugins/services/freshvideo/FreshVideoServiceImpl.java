package cz.vity.freerapid.plugins.services.freshvideo;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FreshVideoServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FreshVideo";
    }

    @Override
    public String getName() {
        return "freshvideo.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FreshVideoFileRunner();
    }

}
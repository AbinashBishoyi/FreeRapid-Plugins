package cz.vity.freerapid.plugins.services.junkyvideo;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class JunkyVideoServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getServiceTitle() {
        return "JunkyVideo";
    }

    @Override
    public String getName() {
        return "junkyvideo.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new JunkyVideoFileRunner();
    }

}
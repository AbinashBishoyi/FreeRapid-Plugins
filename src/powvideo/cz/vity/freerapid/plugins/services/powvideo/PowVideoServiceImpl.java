package cz.vity.freerapid.plugins.services.powvideo;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class PowVideoServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getServiceTitle() {
        return "PowVideo";
    }

    @Override
    public String getName() {
        return "powvideo.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new PowVideoFileRunner();
    }

}
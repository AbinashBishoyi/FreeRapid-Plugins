package cz.vity.freerapid.plugins.services.videla;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class VidELAServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getServiceTitle() {
        return "VidELA";
    }

    @Override
    public String getName() {
        return "videla.org";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new VidELAFileRunner();
    }

}
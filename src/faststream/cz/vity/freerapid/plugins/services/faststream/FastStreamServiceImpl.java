package cz.vity.freerapid.plugins.services.faststream;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FastStreamServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getServiceTitle() {
        return "FastStream";
    }

    @Override
    public String getName() {
        return "faststream.in";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FastStreamFileRunner();
    }

}
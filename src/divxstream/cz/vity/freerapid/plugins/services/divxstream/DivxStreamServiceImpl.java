package cz.vity.freerapid.plugins.services.divxstream;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class DivxStreamServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getName() {
        return "divxstream.net";
    }

    @Override
    public String getServiceTitle() {
        return "DivxStream";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new DivxStreamFileRunner();
    }

}
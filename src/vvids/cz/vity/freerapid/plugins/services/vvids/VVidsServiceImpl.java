package cz.vity.freerapid.plugins.services.vvids;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class VVidsServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getServiceTitle() {
        return "VVids";
    }

    @Override
    public String getName() {
        return "v-vids.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new VVidsFileRunner();
    }

}
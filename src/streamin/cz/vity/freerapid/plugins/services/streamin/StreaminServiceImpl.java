package cz.vity.freerapid.plugins.services.streamin;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class StreaminServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getServiceTitle() {
        return "Streamin";
    }

    @Override
    public String getName() {
        return "streamin.to";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new StreaminFileRunner();
    }

}
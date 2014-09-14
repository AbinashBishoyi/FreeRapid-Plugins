package cz.vity.freerapid.plugins.services.redovs;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class RedovsServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getServiceTitle() {
        return "Redovs";
    }

    @Override
    public String getName() {
        return "redovs.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new RedovsFileRunner();
    }

}
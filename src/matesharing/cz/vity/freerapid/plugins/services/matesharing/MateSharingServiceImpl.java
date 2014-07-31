package cz.vity.freerapid.plugins.services.matesharing;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class MateSharingServiceImpl extends XFilePlayerServiceImpl {

    @Override
    public String getServiceTitle() {
        return "MateSharing";
    }

    @Override
    public String getName() {
        return "matesharing.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MateSharingFileRunner();
    }

}
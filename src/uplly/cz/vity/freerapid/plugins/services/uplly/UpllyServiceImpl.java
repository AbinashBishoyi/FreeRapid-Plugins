package cz.vity.freerapid.plugins.services.uplly;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class UpllyServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "Uplly";
    }

    @Override
    public String getName() {
        return "uplly.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new UpllyFileRunner();
    }

}
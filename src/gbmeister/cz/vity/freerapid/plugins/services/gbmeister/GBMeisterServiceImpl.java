package cz.vity.freerapid.plugins.services.gbmeister;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class GBMeisterServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "GBMeister";
    }

    @Override
    public String getName() {
        return "gbmeister.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new GBMeisterFileRunner();
    }

}
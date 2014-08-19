package cz.vity.freerapid.plugins.services.divxpress;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class DivXpressServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "DivXpress";
    }

    @Override
    public String getName() {
        return "divxpress.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new DivXpressFileRunner();
    }

}
package cz.vity.freerapid.plugins.services.failid;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class FailidServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "Failid";
    }

    @Override
    public String getName() {
        return "failid.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FailidFileRunner();
    }

}
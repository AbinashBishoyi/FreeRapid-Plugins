package cz.vity.freerapid.plugins.services.downflow;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class DownFlowServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "DownFlow";
    }

    @Override
    public String getName() {
        return "downflow.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new DownFlowFileRunner();
    }

}
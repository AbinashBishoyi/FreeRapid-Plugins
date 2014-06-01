package cz.vity.freerapid.plugins.services.redload;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class RedLoadServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "RedLoad";
    }

    @Override
    public String getName() {
        return "redload.net";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new RedLoadFileRunner();
    }

}